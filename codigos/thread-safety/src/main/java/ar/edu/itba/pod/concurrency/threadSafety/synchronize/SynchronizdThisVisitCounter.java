package ar.edu.itba.pod.concurrency.threadSafety.synchronize;

/**
 * Synchronized with this as mutex
 */
public class SynchronizdThisVisitCounter {

    private int c = 0;

    public void addVisit() {
        synchronized (this) {
            c++;
        }
    }

    public synchronized int getVisits() {
        synchronized (this) {
            return c;
        }
    }

    public int peekVisits() {
        return c;
    }

}
