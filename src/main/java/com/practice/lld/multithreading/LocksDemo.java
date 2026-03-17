package com.practice.lld.multithreading;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;

/**
 * Demo for Advanced Locks in Java
 * <p>
 * Covers:
 * 1. ReentrantLock (basic usage)
 * 2. lock(), lockInterruptibly(), tryLock(), tryLock() with timeout
 * 3. Reentrancy (same thread can acquire lock multiple times)
 * 4. Fairness policy
 * 5. ReadWriteLock (separate read/write access)
 */
public class LocksDemo {

    public static void main(String[] args) {

        System.out.println("----- ReentrantLock Basic Demo -----");
        BankAccountWithLock account = new BankAccountWithLock();

        Runnable task = () -> account.withdraw(60);
        new Thread(task, "T1").start();
        new Thread(task, "T2").start();

        sleep(4000);

        System.out.println("\n----- Reentrancy Demo -----");
        ReentrantExample reentrantExample = new ReentrantExample();
        new Thread(reentrantExample::outerMethod).start();

        sleep(2000);

        System.out.println("\n----- Fairness Demo -----");
        FairLockExample fairLockExample = new FairLockExample();
        for (int i = 1; i <= 3; i++) {
            new Thread(fairLockExample::access, "Thread-" + i).start();
        }

        sleep(3000);

        System.out.println("\n----- ReadWriteLock Demo -----");
        ReadWriteCounter counter = new ReadWriteCounter();
        new Thread(() -> counter.increment(), "Writer-1").start();
        new Thread(() -> System.out.println("Read: " + counter.get()), "Reader-1").start();
        new Thread(() -> System.out.println("Read: " + counter.get()), "Reader-2").start();
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

/**
 * Example: ReentrantLock for mutual exclusion
 */
class BankAccountWithLock {

    private int balance = 100;

    /**
     * Fairness = false (default) → higher throughput, possible starvation
     * Fairness = true → FIFO order (no starvation, slightly slower)
     */
    private final Lock lock = new ReentrantLock();

    public void withdraw(int amount) {
        System.out.println(Thread.currentThread().getName() + " attempting withdrawal");

        try {
            // Try acquiring lock with timeout
            /**
             * Lock acquisition variants (important):
             *
             * 1. lock.lock()
             *    - Blocks indefinitely until lock is acquired
             *    - Thread goes to WAITING state
             *    - Not interruptible while waiting
             *
             * 2. lock.tryLock()
             *    - Non-blocking
             *    - Attempts once → returns immediately (true/false)
             *    - Useful to avoid waiting / implement fallback logic
             *
             * 3. lock.tryLock(timeout, unit)
             *    - Waits up to given time to acquire lock
             *    - Thread goes to TIMED_WAITING
             *    - Interruptible while waiting
             *
             * 4. lock.lockInterruptibly()
             *    - Blocks like lock(), but can be interrupted
             *    - Preferred when you want responsiveness to interrupts
             *
             * Summary:
             * - lock() → wait forever
             * - tryLock() → don't wait
             * - tryLock(timeout) → bounded wait
             * - lockInterruptibly() → wait but allow interruption
             */
            if (lock.tryLock(3, TimeUnit.SECONDS)) {
                try {
                    if (balance >= amount) {
                        System.out.println(Thread.currentThread().getName() + " processing...");
                        Thread.sleep(1000);
                        balance -= amount;
                        System.out.println(Thread.currentThread().getName()
                                + " success, remaining: " + balance);
                    } else {
                        System.out.println(Thread.currentThread().getName() + " insufficient balance");
                    }
                } finally {
                    lock.unlock(); // IMPORTANT: always unlock in finally
                }
            } else {
                System.out.println(Thread.currentThread().getName() + " could not acquire lock");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

/**
 * Reentrancy Example
 * <p>
 * Same thread can acquire same lock multiple times
 * Must unlock same number of times → else deadlock
 */
class ReentrantExample {

    private final ReentrantLock lock = new ReentrantLock();

    public void outerMethod() {
        lock.lock();
        try {
            System.out.println("Outer method lock acquired");

            innerMethod(); // same thread re-acquires lock

        } finally {
            lock.unlock(); // first unlock
        }
    }

    private void innerMethod() {
        lock.lock(); // second lock (reentrant)
        try {
            System.out.println("Inner method lock acquired");
        } finally {
            lock.unlock(); // second unlock
        }
    }
}

/**
 * Fair Lock Example
 * <p>
 * Fair lock ensures threads acquire lock in order of request (FIFO)
 */
class FairLockExample {

    private final Lock fairLock = new ReentrantLock(true); // fairness = true

    public void access() {
        fairLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " acquired lock");
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            fairLock.unlock();
        }
    }
}

/**
 * ReadWriteLock Example
 * <p>
 * - Multiple readers allowed simultaneously
 * - Only one writer allowed (exclusive)
 * - Writer blocks readers & other writers
 */
class ReadWriteCounter {

    private int count = 0;

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();

    public void increment() {
        writeLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " writing...");
            Thread.sleep(500);
            count++;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            writeLock.unlock();
        }
    }

    public int get() {
        readLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " reading...");
            Thread.sleep(200);
            return count;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return count;
        } finally {
            readLock.unlock();
        }
    }
}