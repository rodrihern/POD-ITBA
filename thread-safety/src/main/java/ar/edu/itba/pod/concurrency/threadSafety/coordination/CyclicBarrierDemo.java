package ar.edu.itba.pod.concurrency.threadSafety.coordination;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class CyclicBarrierDemo {
    private final int N;
    private final float[][] data;
    private final CyclicBarrier barrier;

    class Worker implements Runnable {
        int myRow;

        Worker(int row) {
            myRow = row;
        }

        public void run() {
            while (!done()) {
                processRow(myRow);

                try {
                    barrier.await();
                } catch (InterruptedException ex) {
                    return;
                } catch (BrokenBarrierException ex) {
                    return;
                }
            }
        }

        private boolean done() {
            return false;
        }

        private void processRow(int myRow) {

        }
    }

    public CyclicBarrierDemo(float[][] matrix) {
        data = matrix;
        N = matrix.length;
        barrier = new CyclicBarrier(N,
                new Runnable() {
                    public void run() {
                        mergeRows();
                    }

                    private void mergeRows() {
                    }
                });
        for (int i = 0; i < N; ++i)
            new Thread(new Worker(i)).start();

        waitUntiDone();
    }

    private void waitUntiDone() {

    }
}