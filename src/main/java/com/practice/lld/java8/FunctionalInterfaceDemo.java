package com.practice.lld.java8;

import java.text.Collator;
import java.util.List;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
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


        /**
         * Predicate is a predefined functional interface that
         * takes one argument and return boolean value
         * Mainly used for conditional testing
         *
         * boolean test(T t)
         */
        Predicate<Integer> isEven = (num) -> num % 2 == 0;
        System.out.println(isEven.test(3));
        System.out.println(isEven.test(4));

        /**
         * Function is a predefined functional interface that
         * takes one input and return some output
         * R apply(T t)
         */
        Function<String, Integer> stringLength = String::length;
        System.out.println(stringLength.apply("Abhishek"));

        BiFunction<Integer, Integer, Integer> multiply = (n1, n2) -> n1 * n2;
        System.out.println(multiply.apply(7, 5));

        /**
         * Consumer is a predefined functional interface that
         * takes one input and returns nothing
         * accept(T t)
         */
        Consumer<Integer> consumer = (num) -> System.out.println(Math.pow(num, num));
        consumer.accept(3);

        /**
         * Supplier is a predefined functional interface that
         * does not take any input but returns a value
         * T get()
         */
        Supplier<List<Integer>> supplier = () -> IntStream.rangeClosed(1, 10).boxed().toList();
        System.out.println(supplier.get());
    }
}


/**
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


/**
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