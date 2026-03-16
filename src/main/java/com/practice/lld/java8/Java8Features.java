package com.practice.lld.java8;


/**
 * Default and Static methods in Interfaces
 * Functional Interfaces
 * Lambada Expression
 * Stream API
 * Method Reference
 * Optional class
 * A new Date-Time API
 */

public class Java8Features {

    public static void main(String[] args) {

        TestInterfaceStaticMethod obj1 = new TestInterfaceStaticMethod();
        obj1.fun();

    }

}


/**
 * Default method in Interface
 *
 * Common implementation can be given so implementation class
 * need not provide implementation though can override
 */
interface InterfaceDefaultMethod {

    default void print() {
        System.out.println("Default method of interface");
    }
}

/**
 * Static method in Interface
 *
 * It belongs to interface not to child class
 */
interface InterfaceStaticMethod {

    // fields are by default static
    int factor = 100;

    static int sum(int a, int b) {
        return a+b;
    }

    static int meterToCm(int len) {
        return len * factor;
    }
}


class TestInterfaceStaticMethod implements InterfaceStaticMethod {

    void fun() {
        System.out.println(factor);
        System.out.println(InterfaceStaticMethod.sum(7, 6));
        System.out.println(InterfaceStaticMethod.meterToCm(2));
    }
}