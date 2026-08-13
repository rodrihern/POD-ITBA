package ar.edu.itba.pod.concurrency.threadSafety.coordination;

public class ThreadBlocker {
    private boolean shouldWait;
    private final Object lock = new Object();

    void blockThread() {
        synchronized (lock) {
            shouldWait = true;
            while (shouldWait) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void notifyThread() {
        synchronized (lock) {
            shouldWait = false;
            lock.notify();
        }
    }

    private static ThreadBlocker threadBlocker = new ThreadBlocker();

    public static void main(String[] args) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                threadBlocker.blockThread();
            }
        }).start();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                threadBlocker.notifyThread();
            }
        }).start();
    }
}
