package ar.edu.itba.pod.concurrency.threadSafety.locks;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;

public class LongRunningAction implements Runnable {
	private String threadName;
	private Phaser phaser;
	private CountDownLatch latch;

	LongRunningAction(String threadName, Phaser phaser) {
		this.threadName = threadName;
		this.phaser = phaser;
		phaser.register();
		latch = new CountDownLatch(5);
	}

	@Override
	public void run() {
		latch.countDown();
		try {
			latch.await();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		System.out.println("This is phase " + phaser.getPhase());
		System.out.println("Thread " + threadName + " before long running action");
		phaser.arriveAndAwaitAdvance();
		try {
			Thread.sleep(20);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		phaser.arriveAndDeregister();
	}
}