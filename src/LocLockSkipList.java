import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;

public class LocLockSkipList<T extends Comparable<T>> implements LockFreeSet<T> {
    /* Number of levels */
    private static final int MAX_LEVEL = 16;

    private final Node<T> head = new Node<T>();
    private final Node<T> tail = new Node<T>();

    // Logs
    ArrayList<Log.Entry>[] logs;
    long linearizationTime;

    public LocLockSkipList(int threads) {

        // Initialize each thread's log
        ArrayList<Log.Entry>[] logs = new ArrayList[threads];
        for (int i = 0; i < threads; i++) {
            logs[i] = new ArrayList<Log.Entry>();
        }
        this.logs = logs;

        for (int i = 0; i < head.next.length; i++) {
            head.next[i] = new AtomicMarkableReference<LocLockSkipList.Node<T>>(tail, false);
        }
    }

    private static final class Node<T> {
        private final T value;
        private final AtomicMarkableReference<Node<T>>[] next;
        private final int topLevel;

        @SuppressWarnings("unchecked")
        public Node() {
            value = null;
            next = (AtomicMarkableReference<Node<T>>[]) new AtomicMarkableReference[MAX_LEVEL + 1];
            for (int i = 0; i < next.length; i++) {
                next[i] = new AtomicMarkableReference<Node<T>>(null, false);
            }
            topLevel = MAX_LEVEL;
        }

        @SuppressWarnings("unchecked")
        public Node(T x, int height) {
            value = x;
            next = (AtomicMarkableReference<Node<T>>[]) new AtomicMarkableReference[height + 1];
            for (int i = 0; i < next.length; i++) {
                next[i] = new AtomicMarkableReference<Node<T>>(null, false);
            }
            topLevel = height;
        }
    }

    /*
     * Returns a level between 0 to MAX_LEVEL,
     * P[randomLevel() = x] = 1/2^(x+1), for x < MAX_LEVEL.
     */
    private static int randomLevel() {
        int r = ThreadLocalRandom.current().nextInt();
        int level = 0;
        r &= (1 << MAX_LEVEL) - 1;
        while ((r & 1) != 0) {
            r >>>= 1;
            level++;
        }
        return level;
    }

    public boolean add(T x) {
        return add(-1, x);
    }

    @SuppressWarnings("unchecked")
    public boolean add(int threadId, T x) {
        int topLevel = randomLevel();
        int bottomLevel = 0;
        Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];

        while (true) {

            // THIS IS THE LINEARIZATION POINT
            boolean found = find(x, preds, succs);
            linearizationTime = System.nanoTime();

            if (found) {
                // log here
                logs[threadId].add(new Log.Entry("add", new Object[] { threadId, x }, false, linearizationTime));
                return false;

            } else {
                Node<T> newNode = new Node(x, topLevel);
                for (int level = bottomLevel; level <= topLevel; level++) {
                    Node<T> succ = succs[level];
                    newNode.next[level].set(succ, false);
                }

                Node<T> pred = preds[bottomLevel];
                Node<T> succ = succs[bottomLevel];

                // Successful linearization point
                if (!pred.next[bottomLevel].compareAndSet(succ, newNode, false, false)) {
                    continue;
                }
                long linearizationTime = System.nanoTime(); // record time just after

                // log here
                logs[threadId].add(new Log.Entry("add", new Object[] { threadId, x }, true, linearizationTime));

                for (int level = bottomLevel + 1; level <= topLevel; level++) {
                    while (true) {
                        pred = preds[level];
                        succ = succs[level];
                        if (pred.next[level].compareAndSet(succ, newNode, false, false)) {
                            break;
                        }
                        find(x, preds, succs);
                    }
                }

                return true;
            }
        }

    }

    public boolean remove(T x) {
        return remove(-1, x);
    }

    @SuppressWarnings("unchecked")
    public boolean remove(int threadId, T x) {
        int bottomLevel = 0;
        Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T> succ;
        while (true) {

            // UNSUCCESSFUL LINEARIZATION POINT
            boolean found = find(x, preds, succs);
            linearizationTime = System.nanoTime();
            if (!found) {
                // log here
                logs[threadId].add(new Log.Entry("remove", new Object[] { threadId, x }, false, linearizationTime));
                return false;

            } else {
                Node<T> nodeToRemove = succs[bottomLevel];
                for (int level = nodeToRemove.topLevel; level >= bottomLevel + 1; level--) {
                    boolean[] marked = { false };
                    succ = nodeToRemove.next[level].get(marked);
                    while (!marked[0]) {
                        nodeToRemove.next[level].compareAndSet(succ, succ, false, true);
                        succ = nodeToRemove.next[level].get(marked);
                    }
                }
                boolean[] marked = { false };
                succ = nodeToRemove.next[bottomLevel].get(marked);
                while (true) {
                    // Successful linearization point
                    boolean iMarkedIt = nodeToRemove.next[bottomLevel].compareAndSet(succ, succ,
                            false, true);
                    linearizationTime = System.nanoTime();
                    //
                    succ = succs[bottomLevel].next[bottomLevel].get(marked);
                    if (iMarkedIt) {
                        // log here
                        logs[threadId]
                                .add(new Log.Entry("remove", new Object[] { threadId, x }, true, linearizationTime));
                        find(x, preds, succs);
                        return true;
                    } else if (marked[0]) {
                        logs[threadId].add(new Log.Entry("remove", new Object[] { threadId, x }, false));
                        return false;
                    }

                }
            }
        }

    }

    public boolean contains(T x) {
        return contains(-1, x);
    }

    public boolean contains(int threadId, T x) {
        int bottomLevel = 0;
        int key = x.hashCode();
        boolean[] marked = { false };
        Node<T> pred = head;
        Node<T> curr = null;
        Node<T> succ = null;
        for (int level = MAX_LEVEL; level >= bottomLevel; level--) {
            curr = pred.next[level].getReference();
            while (true) {
                succ = curr.next[level].get(marked);
                while (marked[0]) {
                    curr = succ;
                    succ = curr.next[level].get(marked);
                }
                if (curr.value != null && x.compareTo(curr.value) < 0) {
                    pred = curr;
                    curr = succ;
                } else {
                    break;
                }
            }
        }

        // Linearization point before returns - no modification in list
        boolean result = (curr.value != null && x.compareTo(curr.value) == 0);
        logs[threadId].add(new Log.Entry("contains", new Object[] { threadId, x }, result));

        return result;
    }

    private boolean find(T x, Node<T>[] preds, Node<T>[] succs) {
        int bottomLevel = 0;
        boolean[] marked = { false };
        boolean snip;
        Node<T> pred = null;
        Node<T> curr = null;
        Node<T> succ = null;
        retry: while (true) {
            pred = head;
            for (int level = MAX_LEVEL; level >= bottomLevel; level--) {
                curr = pred.next[level].getReference();
                // Successful / unsuccessful add / remove linearization points
                while (true) {
                    succ = curr.next[level].get(marked);
                    while (marked[0]) {
                        snip = pred.next[level].compareAndSet(curr, succ, false, false);
                        if (!snip)
                            continue retry;
                        curr = succ;
                        succ = curr.next[level].get(marked);
                    }
                    if (curr.value != null && x.compareTo(curr.value) < 0) {
                        pred = curr;
                        curr = succ;
                    } else {
                        break;
                    }
                }

                preds[level] = pred;
                succs[level] = curr;
            }

            return curr.value != null && x.compareTo(curr.value) == 0;
        }
    }

    public Log.Entry[] getLog() {

        ArrayList<Log.Entry> allEvents = new ArrayList<>();

        for (ArrayList<Log.Entry> threadEvents : logs) {
            allEvents.addAll(threadEvents);
        }

        Log.Entry[] logArray = allEvents.toArray(new Log.Entry[allEvents.size()]);

        Arrays.sort(logArray, Comparator.comparing(event -> event.linearizationTime));

        // Return new array
        return logArray;
    }

}
