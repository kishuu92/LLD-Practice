package com.practice.lld.multithreading;

/**
 * Demo for Synchronization in Java
 * <p>
 * Key Concepts Covered:
 * 1. Shared Resource
 * - Counter object is shared between multiple threads
 * <p>
 * 2. Race Condition
 * - Multiple threads updating the same variable (count)
 * - Without synchronization → incorrect results
 * <p>
 * 3. Synchronization
 * - synchronized keyword ensures only one thread executes increment() at a time
 * <p>
 * 4. Lock / Monitor (Important Interview Concept)
 * - Every Java object has an intrinsic lock (monitor)
 * - synchronized method → lock is acquired on 'this' object
 * - synchronized static method → lock is acquired on Class object
 * - synchronized block → lock can be explicitly chosen
 */
public class SynchronizationDemo {

    public static void main(String[] args) throws InterruptedException {

        // Shared Resource
        SharedCounter counter = new SharedCounter();

        // Two threads operating on same shared resource
        Thread worker1 = new Thread(new CounterIncrementTask(counter), "Worker-1");
        Thread worker2 = new Thread(new CounterIncrementTask(counter), "Worker-2");

        worker1.start();
        worker2.start();

        worker1.join();
        worker2.join();

        // Expected: 20000 (if properly synchronized)
        System.out.println("Final Count: " + counter.getCount());
    }
}

/**
 * Task that increments the shared counter
 */
class CounterIncrementTask implements Runnable {

    private final SharedCounter counter;

    public CounterIncrementTask(SharedCounter counter) {
        this.counter = counter;
    }

    @Override
    public void run() {
        for (int i = 0; i < 10_000; i++) {
            counter.increment();
        }
    }
}

/**
 * Shared Resource
 * <p>
 * Race Condition:
 * - count++ is NOT atomic (read → modify → write)
 * - Multiple threads can interleave → lost updates
 * <p>
 * Synchronization Details:
 * - synchronized method uses intrinsic lock (monitor) of 'this'
 * - Only one thread can hold this lock at a time → mutual exclusion
 * <p>
 * Equivalent using synchronized block:
 * synchronized(this) {
 * count++;
 * }
 * <p>
 * We can also use:
 * - synchronized(this)         → instance-level locking
 * - synchronized(ClassName.class) → class-level locking (for static/shared across instances)
 * - synchronized(anyObject)    → custom lock object (fine-grained control)
 */
class SharedCounter {

    private int count = 0;

    /**
     * Critical Section
     * <p>
     * Implicit Lock:
     * - Lock is acquired on 'this' (current object)
     * - Other threads trying to enter synchronized methods/blocks on same object will block
     */
    public synchronized void increment() {
        count++;
    }

    public int getCount() {
        return count;
    }
}