package com.practice.lld;

/**
 * Demonstrates thread coordination using wait() and notifyAll()
 * to alternately print odd and even numbers.
 * <p>
 * Key ideas:
 * 1. Separate thread logic avoids a shared "God-function".
 * 2. Thread-local counters eliminate the need for a shared counter.
 * 3. synchronized provides happens-before visibility for isEvenTurn.
 * 4. wait() guarded by while to handle spurious wakeups.
 * 5. Dedicated monitor object prevents accidental lock contention.
 */


public class ThreadCoordinatedPrinter {

    static final int MAX = 10;

    // Monitor object for synchronization (cannot use 'this' in static context)
    // Avoid String literals as locks since they may be shared via string interning
    static final Object LOCK = new Object();

    // volatile not required because all access happens inside synchronized blocks
    // synchronized establishes happens-before relationship ensuring visibility
    static boolean isEvenTurn = false;


    public static void main(String[] args) throws InterruptedException {

        // Not using common counter and not creating common function/class for both threads, so we can extend each separately
        Thread evenThread = new Thread(ThreadCoordinatedPrinter::printEven, "Even Thread");
        Thread oddThread = new Thread(ThreadCoordinatedPrinter::printOdd, "Odd Thread");

        evenThread.start();
        oddThread.start();
        evenThread.join();
        oddThread.join();
    }


    static void printEven() {

        int evenNum = 2;
        while (evenNum <= MAX) {
            synchronized (LOCK) {
                // While ensures correct turn even after spurious wakeup
                while (!isEvenTurn) {
                    try {
                        LOCK.wait();
                    } catch (InterruptedException e) {
                        // JVM clears interrupt flag when InterruptedException is thrown
                        // Restore interrupt status so higher-level code can detect interruption
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                System.out.println(Thread.currentThread().getName() + ": " + evenNum);
                evenNum += 2;
                isEvenTurn = false;
                // notifyAll is safer than notify if additional waiting threads are introduced later
                LOCK.notifyAll();
            }
        }
    }

    static void printOdd() {

        int oddNum = 1;
        while (oddNum <= MAX) {
            synchronized (LOCK) {
                // While ensures correct turn even after spurious wakeup
                while (isEvenTurn) {
                    try {
                        // JVM clears interrupt flag when InterruptedException is thrown
                        // Restore interrupt status so higher-level code can detect interruption
                        LOCK.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                System.out.println(Thread.currentThread().getName() + ": " + oddNum);
                oddNum += 2;
                isEvenTurn = true;
                // notifyAll is safer than notify if additional waiting threads are introduced later
                LOCK.notifyAll();
            }
        }
    }
}


/**
 * ---------------------------------------------
 * DETAILED LEARNING NOTES
 * ---------------------------------------------
 * <p>
 * 1. Independent Logic Streams
 * Intentionally decoupled printEven and printOdd into separate methods.
 * This avoids the "God-function" trap and allows each thread’s logic (increments, data sources, or processing)
 * to be extended independently without impacting the other.
 * <p>
 * 2. Encapsulated Thread State:
 * Each thread maintains its own local tracking variable (oddNum, evenNum).
 * While a shared counter is safe within synchronized blocks, using local variables reduces the dependency
 * between threads and simplifies the mental model for future logic changes (e.g., changing one thread to a different sequence).
 * <p>
 * 3. JMM Visibility & Happens-Before:
 * Correctly utilizes the synchronized monitor to establish a "happens-before" relationship.
 * This ensures that the state of evenTurn is updated and visible to the next thread immediately
 * upon the current thread exiting the monitor.
 * <p>
 * 4. Defensive Synchronization:
 * Uses the standard while loop for lock.wait() to effectively handle spurious wakeups
 * and ensures the InterruptedException is handled by restoring the interrupt status rather than swallowing it.
 * <p>
 * 5. Static Monitor Safety:
 * Uses a dedicated static final Object as a lock. This is a best practice to avoid "lock contention" or
 * accidental deadlocks that can occur when locking on this, a Class object, or interned Strings.
 */