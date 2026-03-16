package com.practice.lld.java8;

public class FunctionalInterfaceDemo {

    public static void main(String[] args) {

        // Anonymous inner class
        Task task1 = new Task() {
            @Override
            public void execute() {
                System.out.println("Task1 executing");
            }
        };
        task1.execute();
        task1.print();
        System.out.println();

        // Lambda expression
        Task task2 = () -> System.out.println("Task2 executing");
        task2.execute();
        task2.print();
        System.out.println();

        Task task3 = () -> {
            System.out.println("Task3 started");
            System.out.println("Task3 completed");
        };
        task3.execute();
        task3.print();
        System.out.println();

        Compute square = k -> {return k*k;};
        System.out.println(square.solve(5));

        Compute cube = k -> {return k*k*k;};
        System.out.println(cube.solve(3));
    }
}


/**
 * Should have exactly one abstract method,
 * but we can have other default method
 */
@FunctionalInterface
interface Task {

    void execute();

    default void print() {
        System.out.println("Default print method of functional interface Task");
    }
}


interface Compute {

    int solve(int x);

}


