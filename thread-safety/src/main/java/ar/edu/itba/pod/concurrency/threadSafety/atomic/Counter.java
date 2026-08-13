package ar.edu.itba.pod.concurrency.threadSafety.atomic;

/**
 * Counter interface.
 */
public interface Counter {
    long getCounter();

    void increment();
}
