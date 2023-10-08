import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.Arrays;

public class Measurement {

    private static Distribution getOps(String type, int numOps) {
        int[] probability;
        switch (type) {
            case "A1":
                probability = new int[] { 1, 1, 8 };
                return new Distribution.Discrete(42, probability);
            case "A2":
                probability = new int[] { 1, 1 };
                return new Distribution.Discrete(42, probability);
            case "B1":
                probability = new int[] { 1, 1, 8 };
                return new Distribution.Discrete(42, probability);
            case "B2":
                probability = new int[] { 1, 1 };
                return new Distribution.Discrete(42, probability);
            default:
                return null;
        }
    }

    private static Distribution getValues(String type, int max) {
        switch (type) {
            case "A1":
                return new Distribution.Uniform(82, 0, max);
            case "A2":
                return new Distribution.Uniform(82, 0, max);
            case "B1":
                return new Distribution.Normal(82, 100, 0, max);
            case "B2":
                return new Distribution.Normal(82, 100, 0, max);
            default:
                return null;
        }
    }

    public static void measure(String type, int threads, int numValues, int numOps, int max, boolean validateLog) {

        try {
            // Create a standard lock free skip list
            LockFreeSet<Integer> lockFreeSet = new LockFreeSkipList<>();

            // Get ops and values for selected execution type
            Distribution ops = Measurement.getOps(type, numOps);
            Distribution values = Measurement.getValues(type, max);

            // Run experiment with n threads
            run_measurement(threads, lockFreeSet, ops, values);

            if (validateLog) {
                // Get the log
                Log.Entry[] log = lockFreeSet.getLog();

                // Check sequential consistency
                Log.validate(log);
            }

            // Print information to stderr
            System.err.printf("Execution type:    %s\n", type);
            System.err.printf("Thread count:      %d\n", threads);
            System.err.printf("Number of values:  %d\n", numValues);
            System.err.printf("Number of ops:     %d\n", numOps);
            System.err.printf("Max value:         %d\n", max);

            // Record execTime
            double execTime = run_measurement(threads, lockFreeSet, ops, values);

            // Prints average execution time and standard deviation to stdout.
            System.out.printf("%s %d %.2f\n", type, threads, execTime);

            System.err.println("Measurements done");

        } catch (

        Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static long run_measurement(int threads, LockFreeSet<Integer> list, Distribution ops, Distribution values)
            throws Exception {

        ExecutorService executorService = Executors.newFixedThreadPool(threads);

        Task[] tasks = new Task[threads];
        for (int i = 0; i < tasks.length; ++i) {
            tasks[i] = new Task(i, list, ops.copy(i), values.copy(-i));
        }

        long startTime = System.nanoTime();
        executorService.invokeAll(Arrays.asList(tasks));
        long endTime = System.nanoTime();
        executorService.shutdown();

        return endTime - startTime;
    }

    public static class Task implements Callable<Void> {
        private final int threadId;
        private final LockFreeSet<Integer> set;
        private final Distribution ops, values;

        public Task(int threadId, LockFreeSet<Integer> set, Distribution ops, Distribution values) {
            this.threadId = threadId;
            this.set = set;
            this.ops = ops;
            this.values = values;

        }

        public Void call() throws Exception {
            for (int i = 0; i < 100_000; ++i) {
                int val = values.next();
                int op = ops.next();
                switch (op) {
                    case 0:
                        // System.out.println("I am adding");
                        set.add(threadId, val);
                        break;
                    case 1:
                        // System.out.println("I am removing");
                        set.remove(threadId, val);
                        break;
                    case 2:
                        // System.out.println("I am containing");
                        set.contains(threadId, val);
                        break;
                }
            }
            return null;
        }

    }

    public static void main(String[] args) {

        // Execution type - A1, A2, B1, B2
        String type = args[0];
        // Number of threads.
        int threads = Integer.parseInt(args[1]);
        // Number of values
        int numValues = Integer.parseInt(args[2]);
        // Number of ops
        int numOps = Integer.parseInt(args[3]);
        // Max sampling number
        int max = Integer.parseInt(args[4]);
        // Shoull we validate log?
        boolean validateLog = Boolean.parseBoolean(args[5]);

        // Take measurements
        measure(type, threads, numValues, numOps, max, validateLog);

    }
}
