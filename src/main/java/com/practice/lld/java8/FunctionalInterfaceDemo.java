package com.practice.lld.java8;

/*
 * Demonstrates Functional Interfaces in Java 8.
 *
 * Key points:
 * - A functional interface must have exactly one abstract method
 * - It can also have multiple default and static methods
 * - Lambda expressions work with functional interfaces
 * - @FunctionalInterface annotation is recommended (compile-time check)
 */
public class FunctionalInterfaceDemo {

    public static void main(String[] args) {

        // Using anonymous inner class (pre Java 8 style)
        Task task1 = new Task() {
            @Override
            public void execute() {
                System.out.println("Task1 executing");
            }
        };

        task1.execute();
        task1.print();

        System.out.println();


        // Lambda expression - shorter syntax
        Task task2 = () -> System.out.println("Task2 executing");

        task2.execute();
        task2.print();

        System.out.println();


        // Lambda with multiple statements
        Task task3 = () -> {
            System.out.println("Task3 started");
            System.out.println("Task3 completed");
        };

        task3.execute();
        task3.print();

        System.out.println();


        // Functional interface for mathematical operations
        MathOperation square = x -> x * x;
        System.out.println(square.apply(5));

        MathOperation cube = x -> x * x * x;
        System.out.println(cube.apply(3));

        // Static method in functional interface
        MathOperation.describe();
    }
}


/*
 * Example functional interface.
 *
 * Only one abstract method is allowed.
 * Default and static methods are allowed.
 */
@FunctionalInterface
interface Task {

    void execute();   // single abstract method

    // default method with implementation
    default void print() {
        System.out.println("Default method from Task interface");
    }
}


/*
 * Another functional interface example
 * used with lambda expressions.
 */
@FunctionalInterface
interface MathOperation {

    int apply(int x);

    static void describe() {
        System.out.println("Performs a mathematical operation on a number");
    }
}