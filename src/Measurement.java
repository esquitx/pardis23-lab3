import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Measurement {

    public static void warmup(int warmUp) {

        Set<Integer> seqSet = new HashSet<>();
        LockFreeSet<Integer> lockFreeSet = new LockFreeSkipList<>();

        // WarmUp JVM, but also test LockFreeSkipList implementation
        for (int k = 0; k < warmUp; k++) {
            Random rng = new Random();
            for (int i = 0; i < 1_000_000; i++) {
                int val = rng.nextInt(100);
                int op = rng.nextInt(3);
                String opName;
                boolean resSeq, resLockFree;
                if (op == 0) {
                    resSeq = seqSet.add(val);
                    resLockFree = lockFreeSet.add(val);
                    opName = "add";
                } else if (op == 1) {
                    resSeq = seqSet.remove(val);
                    resLockFree = lockFreeSet.remove(val);
                    opName = "remove";
                } else {
                    resSeq = seqSet.contains(val);
                    resLockFree = lockFreeSet.contains(val);
                    opName = "contains";
                }

                if (resSeq == resLockFree)
                    continue;
                System.out.println(": value of lock free (" + resLockFree + ") not matching sequential ("
                        + resSeq + ")");
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void measure(String sampling, String type, int threads, int numOps, int max) {

        try {
            // Create a standard lock free skip list
            LockFreeSet<Integer> lockFreeSet = Auxiliary.getSet(sampling, threads);

            // Get ops and values for selected execution type
            Distribution ops = Auxiliary.getOps(type, numOps, 42);
            Distribution values = Auxiliary.getValues(type, max, 42);

            // Record execTime
            double execTime = run_measurement(threads, lockFreeSet, ops, values, numOps);

            // Round complete. Proceding to validation.
            Log.Entry[] log = lockFreeSet.getLog();
            int discrepancies = Log.validate(log);

            // Calculate accuracy
            double accuracy = 1 - (discrepancies / (numOps * threads));

            // Output results to console
            System.out.printf("%s %d %.2f %d %.4f \n", type, threads, execTime, discrepancies, accuracy);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    public static long run_measurement(int threads, LockFreeSet<Integer> list, Distribution ops, Distribution values,
            int numOps)
            throws Exception {

        ExecutorService executorService = Executors.newFixedThreadPool(threads);

        Task[] tasks = new Task[threads];
        for (int i = 0; i < tasks.length; ++i) {
            tasks[i] = new Task(i, list, ops.copy(i), values.copy(-i), numOps);
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
        private final int numOps;

        public Task(int threadId, LockFreeSet<Integer> set, Distribution ops, Distribution values, int numOps) {
            this.threadId = threadId;
            this.set = set;
            this.ops = ops;
            this.values = values;
            this.numOps = numOps;

        }

        public Void call() throws Exception {
            boolean output;
            for (int i = 0; i < numOps; ++i) {
                int val = values.next();
                int op = ops.next();
                switch (op) {
                    case 0:
                        // System.out.println("I am adding");
                        output = set.add(threadId, val);
                        // System.out.println(threadId + " added: " + val);
                        // System.out.println(output);
                        break;
                    case 1:
                        // System.out.println("I am removing");
                        output = set.remove(threadId, val);
                        // System.out.println(threadId + " removed: " + val);
                        // System.out.println(output);
                        break;
                    case 2:
                        // System.out.println("I am containing");
                        output = set.contains(threadId, val);
                        // System.out.println(threadId + " checks contains " + val);
                        // System.out.println(output);
                        break;
                }
            }
            return null;
        }

    }

    public static void main(String[] args) {

        // Sampling type
        String sampling = args[0];
        // Execution type - A1, A2, B1, B2
        String type = args[1];
        // Number of threads.
        int threads = Integer.parseInt(args[2]);
        // Number of ops
        int numOps = Integer.parseInt(args[3]);
        // Max sampling number
        int max = Integer.parseInt(args[4]);

        // Print information to stderr
        System.err.printf("Execution type:    %s\n", type);
        System.err.printf("Thread count:      %d\n", threads);
        System.err.printf("Number of ops:     %d\n", numOps);
        System.err.printf("Max value:         %d\n", max);

        // Warmup JVM
        int warmUp = 30; // Number of warmup rounds
        System.err.println("Warming up...");

        warmup(warmUp);
        System.err.println("Warmup COMPLETE.");

        // Take measurements
        System.err.println("Taking measurements...");
        measure(sampling, type, threads, numOps, max);
        // double[] results = Auxiliary.getMeanAndStDev(measurements);

    }
}
