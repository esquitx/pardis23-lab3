public class Auxiliary {

    public static Distribution getOps(String type, int numOps) {
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

    public static Distribution getValues(String type, int max) {
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
}
