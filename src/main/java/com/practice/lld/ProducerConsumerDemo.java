package com.practice.lld;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


/**
 * Entry point for the Producer–Consumer demonstration.
 * <p>
 * This demo shows two implementations of a bounded buffer:
 * <p>
 * 1. Wait/Notify based implementation
 * - Uses intrinsic locks (synchronized)
 * - Demonstrates classic concurrency coordination.
 * <p>
 * 2. BlockingQueue based implementation
 * - Uses java.util.concurrent utilities
 * - Represents the preferred production approach.
 * <p>
 * Producers generate messages and place them into the buffer,
 * while consumers retrieve and process them concurrently.
 * <p>
 * The ProducerConsumerManager is responsible for starting,
 * managing, and shutting down worker threads.
 */
public class ProducerConsumerDemo {

    public static void main(String[] args) throws InterruptedException {

        System.out.println();
        System.out.println("---------Demo using simple queue with wait and notify---------");
        System.out.println();

        Buffer buffer = new WaitNotifyBoundedBuffer(SystemConfig.BUFFER_CAPACITY.getValue());
        ProducerConsumerManager manager = new ProducerConsumerManager(buffer);
        manager.startConsumers(SystemConfig.CONSUMER_COUNT.getValue());
        manager.startProducers(SystemConfig.PRODUCER_COUNT.getValue());

        Thread.sleep(20_000);
        manager.shutdown();
        manager.waitForCompletion();

        System.out.println();
        System.out.println("---------Demo using blocking queue---------");
        System.out.println();

        buffer = new BlockingQueueBoundedBuffer(SystemConfig.BUFFER_CAPACITY.getValue());
        manager = new ProducerConsumerManager(buffer);
        manager.startConsumers(SystemConfig.CONSUMER_COUNT.getValue());
        manager.startProducers(SystemConfig.PRODUCER_COUNT.getValue());

        Thread.sleep(20_000);
        manager.shutdown();
        manager.waitForCompletion();
    }
}

/**
 * Enum representing the role of a worker in the system.
 * <p>
 * Used primarily for clarity and logging to distinguish
 * between producer and consumer threads.
 */
enum WorkerType {
    PRODUCER,
    CONSUMER
}

/**
 * Configuration values used by the Producer–Consumer system.
 * <p>
 * These constants control:
 * - Buffer capacity
 * - Number of producer threads
 * - Number of consumer threads
 * <p>
 * In real applications, such configuration would typically
 * come from external configuration files or environment variables.
 */
@AllArgsConstructor
@Getter
enum SystemConfig {

    BUFFER_CAPACITY(5),
    PRODUCER_COUNT(2),
    CONSUMER_COUNT(2);

    private final int value;
}

/**
 * Immutable data object representing a message exchanged
 * between producers and consumers.
 * <p>
 * Implemented as a Java record since it is a simple
 * data carrier with no behavior.
 */
record Message(String id, String payload) {
}


/**
 * Buffer abstraction shared between producers and consumers.
 * <p>
 * Multiple implementations can exist:
 * 1. Wait/Notify based buffer (educational)
 * 2. BlockingQueue based buffer (production)
 */
interface Buffer {

    void put(Message message) throws InterruptedException;

    Message take() throws InterruptedException;

    int size();
}


/**
 * Bounded buffer implementation using classic wait/notify pattern.
 * <p>
 * Producers block when buffer is full.
 * Consumers block when buffer is empty.
 * <p>
 * Uses intrinsic monitor lock via synchronized.
 */
@AllArgsConstructor
class WaitNotifyBoundedBuffer implements Buffer {

    private final Queue<Message> queue = new LinkedList<>();
    private final int capacity;

    @Override
    public synchronized void put(Message message) throws InterruptedException {

        while (queue.size() == capacity) {
            wait();
        }
        queue.add(message);
        notifyAll();
    }

    @Override
    public synchronized Message take() throws InterruptedException {

        while (queue.isEmpty()) {
            wait();
        }
        Message message = queue.poll();
        notifyAll();
        return message;
    }

    @Override
    public int size() {
        return queue.size();
    }
}


/**
 * Production-grade buffer implementation using BlockingQueue.
 * <p>
 * Delegates synchronization and blocking behavior
 * to Java's concurrent utilities.
 */
class BlockingQueueBoundedBuffer implements Buffer {

    private final BlockingQueue<Message> queue;

    public BlockingQueueBoundedBuffer(int capacity) {
        queue = new ArrayBlockingQueue<>(capacity);
    }

    @Override
    public void put(Message message) throws InterruptedException {
        queue.put(message);
    }

    @Override
    public Message take() throws InterruptedException {
        return queue.take();
    }

    @Override
    public int size() {
        return queue.size();
    }
}

/**
 * Base abstraction for all worker threads in the system.
 * <p>
 * Both Producer and Consumer extend this class.
 * <p>
 * Encapsulates shared properties:
 * - workerId   : unique identifier for logging/debugging
 * - buffer     : shared buffer used for communication
 * - workerType : indicates producer or consumer role
 * <p>
 * Subclasses implement the run() method to define
 * their specific behavior.
 */
@AllArgsConstructor
@Getter
abstract class Worker implements Runnable {

    protected final String workerId;
    protected final Buffer buffer;
    protected final WorkerType workerType;
}

/**
 * Producer generates messages and puts them into the buffer.
 * Simulates work using random sleep.
 */
class Producer extends Worker {

    public Producer(String workerId, Buffer buffer, WorkerType workerType) {
        super(workerId, buffer, workerType);
    }

    @Override
    public void run() {

        try {
            for (int i = 1; i < 20; i++) {
                Thread.sleep((long) (Math.random() * 1000));
                Message message = new Message("msg-" + i, "data");
                System.out.println(workerId + " produced " + message.id());
                buffer.put(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

/**
 * Consumer retrieves messages from the buffer and processes them.
 * Thread exits when interrupted by the manager.
 */
class Consumer extends Worker {

    public Consumer(String workerId, Buffer buffer, WorkerType workerType) {
        super(workerId, buffer, workerType);
    }

    @Override
    public void run() {

        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep((long) (Math.random() * 1000));
                Message message = buffer.take();
                System.out.println("\t" + workerId + " consumed " + message.id());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }
}

/**
 * Manages lifecycle of producer and consumer threads.
 * <p>
 * Responsible for:
 * - starting workers
 * - stopping workers
 * - waiting for completion
 */
@AllArgsConstructor
class ProducerConsumerManager {

    private final Buffer buffer;
    private final List<Thread> workers = new ArrayList<>();

    public void startProducers(int count) {

        for (int i = 1; i <= count; i++) {

            Producer producer = new Producer("Producer-" + i, buffer, WorkerType.PRODUCER);
            Thread thread = new Thread(producer);

            workers.add(thread);
            thread.start();
        }

    }

    public void startConsumers(int count) {

        for (int i = 1; i <= count; i++) {

            Consumer consumer = new Consumer("Consumer-" + i, buffer, WorkerType.CONSUMER);
            Thread thread = new Thread(consumer);

            workers.add(thread);
            thread.start();
        }

    }

    public void waitForCompletion() {

        for (Thread thread : workers) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void shutdown() {

        for (Thread thread : workers) {
            thread.interrupt();
        }
    }
}
