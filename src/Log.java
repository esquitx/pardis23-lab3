import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Log {

        // keep track of log entries
        public List<Log.Entry> list = new ArrayList<Log.Entry>();

        private Log() {
                // Do not implement
        }

        public static boolean validate(Log.Entry[] log) {

                HashSet<Object> set = new HashSet<>();
                int discrepancies = 0;

                for (Log.Entry event : log) {

                        boolean seqReturn = false;
                        switch (event.methodName) {
                                case "add":
                                        seqReturn = set.add(event.arguments[1]);
                                        break;
                                case "remove":
                                        seqReturn = set.remove(event.arguments[1]);
                                        break;
                                case "contains":
                                        seqReturn = set.contains(event.arguments[1]);
                                        break;
                                default:
                                        System.err.printf("%s is not a method name not identified. Check for errors",
                                                        event.methodName);
                                        System.exit(1);

                        }

                        if (seqReturn != event.returnValue) {
                                discrepancies++;
                        }
                }

                System.err.printf("Enconuntered %d discrepancies\n", discrepancies);

                return discrepancies == 0;
        }

        // Log entry for linearization point.
        public static class Entry {

                String methodName;
                Object[] arguments;
                boolean returnValue;
                long linearizationTime;

                public Entry(String methodName, Object[] arguments, boolean returnValue) {
                        this.methodName = methodName;
                        this.arguments = arguments;
                        this.returnValue = returnValue;
                        this.linearizationTime = System.nanoTime();

                }
        }

}
