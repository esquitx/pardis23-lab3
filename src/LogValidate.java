import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LogValidate {
        try
        {
                // Create a standard lock free skip list
                LockFreeSet<Integer> lockFreeSet = new LockFreeSkipList<>();

                // Get ops and values for selected execution type
                Distribution ops = TimeMeasurement.getOps(type, numOps);
                Distribution values = TimeMeasurement.getValues(type, max);

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

        }catch(

        Exception e)
        {
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
}