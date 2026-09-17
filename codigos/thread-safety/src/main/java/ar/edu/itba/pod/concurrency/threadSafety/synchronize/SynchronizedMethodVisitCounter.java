package ar.edu.itba.pod.concurrency.threadSafety.synchronize;

/**
 * 
 */
public class SynchronizedMethodVisitCounter {
    private int c = 0;

    public synchronized void addVisit() {
        c++;
    }

    public synchronized int getVisits() {
        return c;
    }

    public int peekVisits() {
        return c;
    }
}
