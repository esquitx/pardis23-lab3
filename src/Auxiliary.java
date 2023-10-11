public class Auxiliary {

    private static final int NUM_SAMPLES = 1_000;

    public static LockFreeSet getSet(String type) {
        switch (type) {
            case "basic":
                return new LockFreeSkipList<>();
            case "globLock":
                return new GlobLockSkipList();
            case "locLock":
                return new LocLockSkipList();
            case "nolock":
                return new NoLockSkipList<>();
            default:
                return null;
        }
    }

    public static Distribution getOps(String type, int numOps, int seed) {
        int[] probability;
        switch (type) {
            case "A1":
                probability = new int[] { 1, 1, 8 };
                return new Distribution.Discrete(seed, probability);
            case "A2":
                probability = new int[] { 1, 1, 0 };
                return new Distribution.Discrete(seed, probability);
            case "B1":
                probability = new int[] { 1, 1, 8 };
                return new Distribution.Discrete(seed, probability);
            case "B2":
                probability = new int[] { 1, 1, 0 };
                return new Distribution.Discrete(seed, probability);
            case "testAdd":
                probability = new int[] { 1, 0, 0 };
                return new Distribution.Discrete(seed, probability);
            case "testRemove":
                probability = new int[] { 1, 9, 0 };
                return new Distribution.Discrete(seed, probability);
            case "testContains":
                probability = new int[] { 1, 0, 9 };
                return new Distribution.Discrete(seed, probability);
            default:
                return null;
        }
    }

    public static Distribution getValues(String type, int max, int seed) {
        switch (type) {
            case "A1":
                return new Distribution.Uniform(seed, 0, max);
            case "A2":
                return new Distribution.Uniform(seed, 0, max);
            case "B1":
                return new Distribution.Normal(seed, NUM_SAMPLES, 0, max);
            case "B2":
                return new Distribution.Normal(seed, NUM_SAMPLES, 0, max);
            case "testAdd":
            case "testRemove":
            case "testContains": {
                return new Distribution.Uniform(seed, 0, max);
            }
            default:
                return null;
        }
    }

    public static double[] getMeanAndStDev(long[] data) {

        // Calculate mean
        double sum = 0.0;
        for (double time : data) {
            sum += time;
        }
        double mean = (sum / data.length);

        // Calculate stdev (from variance)
        double variance = 0.0;
        for (double time : data) {
            variance += Math.pow(time - mean, 2);
        }
        double stdev = Math.sqrt(variance / data.length);

        double[] results = { mean, stdev };
        return results;
    }

}
