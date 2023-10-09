import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class LockFreeSkipList<T extends Comparable<T>> implements LockFreeSet<T> {
        /* Number of levels */
        private static final int MAX_LEVEL = 16;

        private final Node<T> head = new Node<T>();
        private final Node<T> tail = new Node<T>();

        // log
        private ArrayList<Log.Entry> log = new ArrayList<Log.Entry>();

        public LockFreeSkipList() {
                for (int i = 0; i < head.next.length; i++) {
                        head.next[i] = new AtomicMarkableReference<LockFreeSkipList.Node<T>>(tail, false);
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
                // Start traversing list
                while (true) {
                        // Check if element already added, end if so
                        boolean found = find(x, preds, succs);
                        if (found) {
                                return false;
                        } else {
                                Node<T> newNode = new Node(x, topLevel); // Create the new node
                                // Traverse list in order to find place to add
                                for (int level = bottomLevel; level <= topLevel; level++) {
                                        Node<T> succ = succs[level];
                                        newNode.next[level].set(succ, false);
                                }
                                Node<T> pred = preds[bottomLevel];
                                Node<T> succ = succs[bottomLevel];
                                // If not reached bottom level and not marked, skip
                                if (!pred.next[bottomLevel].compareAndSet(succ, newNode, false, false)) {
                                        synchronized (log) {
                                                log.add(new Log.Entry("add", new Object[] { threadId, x }, false));
                                        }
                                        continue;
                                }
                                // LinPoint - Global lock
                                synchronized (log) {
                                        log.add(new Log.Entry("add", new Object[] { threadId, x }, true));
                                }
                                // Traverse levels
                                for (int level = bottomLevel + 1; level <= topLevel; level++) {
                                        while (true) {
                                                pred = preds[level];
                                                succ = succs[level];
                                                // If found place in next level, stop.
                                                if (pred.next[level].compareAndSet(succ, newNode, false, false))
                                                        break;
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
                        boolean found = find(x, preds, succs);
                        if (!found) {
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
                                        boolean iMarkedIt = nodeToRemove.next[bottomLevel].compareAndSet(succ, succ,
                                                        false, true);
                                        succ = succs[bottomLevel].next[bottomLevel].get(marked);
                                        if (iMarkedIt) {
                                                synchronized (log) {
                                                        log.add(new Log.Entry("remove", new Object[] { threadId, x },
                                                                        true));
                                                }

                                                find(x, preds, succs);
                                                return true;
                                        } else if (marked[0]) {
                                                synchronized (log) {
                                                        log.add(new Log.Entry("remove", new Object[] { threadId, x },
                                                                        false));
                                                }
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

                // boolean result
                boolean result = false;
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
                                        result = curr.value != null && x.compareTo(curr.value) == 0 && !marked[0];
                                        synchronized (log) {
                                                log.add(new Log.Entry("contains", new Object[] { threadId, x },
                                                                result));
                                        }
                                        break;
                                }
                        }
                }
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

                // Convert to array
                return null;
        }

}
