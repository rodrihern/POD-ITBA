package ar.edu.itba.pod.concurrency.threadSafety.locks;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReentrantReadWriteLockExample {

	private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
	private static String message = "0";
	
	private static AtomicBoolean atmBool;
	private static AtomicLong atmLong;
	
	public static void main(String[] args) {
		atmBool = new AtomicBoolean(true);
		atmLong = new AtomicLong(1);
		
//		atmLong.
		boolean b = atmBool.get();
		atmBool.set(false);
		atmBool.compareAndSet(true, false);
	}

	static class Read implements Runnable {

		public void run() {
			
			
			
			for (int i = 0; i <= 10; i++) {
				if (lock.isWriteLocked()) {
					System.out.println("I'll take the lock from Write");
				}
				lock.readLock().lock();
				System.out.println("ReadThread " + Thread.currentThread().getId() + " ---> Message is " + message);
				lock.readLock().unlock();
			}
		}
	}

	static class WriteA implements Runnable {

		public void run() {
			for (int i = 0; i <= 10; i++) {
				try {
					lock.writeLock().lock();
					message = message.concat("1");
				} finally {
					lock.writeLock().unlock();
				}
			}
		}
	}

	static class WriteB implements Runnable {

		public void run() {
			for (int i = 0; i <= 10; i++) {
				try {
					lock.writeLock().lock();
					message = message.concat("2");
				} finally {
					lock.writeLock().unlock();
				}
			}
		}
	}
}