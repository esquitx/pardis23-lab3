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
                int discrepancyCount = 0;

                for (Log.Entry event : log) {

                        if (event.methodName.equals("add")) {
                                boolean realReturnValue = set.add(event.arguments[0]);
                                if (realReturnValue != (boolean) event.returnValue) {
                                        discrepancyCount++;
                                }

                        } else if (event.methodName.equals("remove")) {
                                boolean realReturnValue = set.remove(event.arguments[0]);
                                if (realReturnValue != (boolean) event.returnValue) {
                                        discrepancyCount++;
                                }

                        } else if (event.methodName.equals("contains")) {
                                boolean realReturnValue = set.contains(event.arguments[0]);
                                if (realReturnValue != (boolean) event.returnValue) {
                                        discrepancyCount++;
                                }
                        }
                }

                // Notify discrepancy count in terminal
                System.err.printf("Encountered %d discrepancies\n", discrepancyCount);

                return discrepancyCount == 0;
        }

        // Log entry for linearization point.
        public static class Entry {

                String methodName;
                Object[] arguments;
                Object returnValue;
                long linearizationTime;

                public Entry(String methodName, Object[] arguments, Object returnValue) {
                        this.methodName = methodName;
                        this.arguments = arguments;
                        this.returnValue = returnValue;
                        this.linearizationTime = System.nanoTime();

                }
        }

}
