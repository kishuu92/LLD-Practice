package com.practice.lld.multithreading;

/**
 * Demo for basic Java Thread concepts:
 * - Thread creation via extending Thread
 * - Thread creation via implementing Runnable
 * - Thread creation via lambda
 * - Thread lifecycle methods: start(), join()
 * - Thread states: NEW, RUNNABLE, TIMED_WAITING, TERMINATED
 * - Common Thread methods
 */
public class ThreadDemo {

    public static void main(String[] args) throws InterruptedException {

        // 1. Thread creation by extending Thread class
        Thread helloThread = new HelloThread();
        helloThread.start();
        helloThread.join();
        System.out.println("HelloThread completed\n");

        // 2. Thread creation by implementing Runnable interface
        Thread hiThread = new Thread(new HiRunnableThread());
        hiThread.start();
        hiThread.join();
        System.out.println("HiRunnableThread completed\n");

        // 3. Thread creation using lambda expression
        Thread byeThread = new Thread(() -> System.out.println("Bye"));
        byeThread.start();
        byeThread.join();
        System.out.println("LambdaThread completed\n");

        // 4. Demonstrating thread states
        Thread stateThread = new Thread(() -> {
            try {
                Thread.sleep(1000); // TIMED_WAITING
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        System.out.println("State before start(): " + stateThread.getState()); // NEW
        stateThread.start();
        System.out.println("State just after start(): " + stateThread.getState()); // RUNNABLE
        Thread.sleep(100);
        System.out.println("State after 100ms: " + stateThread.getState()); // TIMED_WAITING
        stateThread.join();
        System.out.println("State after join(): " + stateThread.getState()); // TERMINATED

        System.out.println("\n---------------- Thread Methods Demo ----------------\n");

        // 5. Thread methods demo

        Thread demoThread = new Thread(() -> {
            System.out.println("Running in: " + Thread.currentThread().getName());
            // yield() hint to scheduler
            Thread.yield();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.out.println("Thread interrupted!");
                Thread.currentThread().interrupt();
            }
        });

        // setName()
        demoThread.setName("Demo-Thread");

        // setPriority() (1 to 10, default 5)
        demoThread.setPriority(Thread.NORM_PRIORITY + 1);

        // setDaemon() -> must be before start()
        demoThread.setDaemon(false);

        System.out.println("Thread name: " + demoThread.getName());
        System.out.println("Thread priority: " + demoThread.getPriority());
        System.out.println("Is daemon: " + demoThread.isDaemon());

        demoThread.start();

        // isAlive()
        System.out.println("Is alive after start: " + demoThread.isAlive());

        // interrupt()
        Thread.sleep(100);
        demoThread.interrupt();

        demoThread.join();
        System.out.println("Is alive after completion: " + demoThread.isAlive());

        System.out.println("\nMain thread: " + Thread.currentThread().getName());
    }
}

/**
 * Thread created by extending Thread class
 */
class HelloThread extends Thread {
    @Override
    public void run() {
        System.out.println("Hello");
    }
}

/**
 * Thread created by implementing Runnable interface
 */
class HiRunnableThread implements Runnable {
    @Override
    public void run() {
        System.out.println("Hi");
    }
}