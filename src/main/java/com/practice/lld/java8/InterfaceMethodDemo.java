package com.practice.lld.java8;

/*
 * Java 8 added two useful capabilities to interfaces:
 *
 * 1. Static methods
 *    - Used for helper / utility logic related to the interface
 *    - Belong to the interface itself (not inherited by implementing classes)
 *
 * 2. Default methods
 *    - Allow providing a method implementation inside an interface
 *    - Helps maintain backward compatibility when adding new methods
 */
public class InterfaceMethodDemo {

    public static void main(String[] args) {

        // Static methods must be called using the interface name
        System.out.println(UnitConverter.FACTOR);
        System.out.println(UnitConverter.add(7, 8));
        System.out.println(UnitConverter.meterToCm(2));

        System.out.println();

        StaticMethodExample obj1 = new StaticMethodExample();
        obj1.test();

        System.out.println();

        // Default method example
        DefaultPrinter printer1 = new BasicPrinter();
        printer1.print();

        DefaultPrinter printer2 = new CustomPrinter();
        printer2.print();
    }
}

/*
 * Static methods in interfaces were introduced in Java 8.
 * They are useful for keeping utility methods related to the interface
 * without forcing implementing classes to implement them.
 */
interface UnitConverter {

    // interface fields are implicitly public static final
    int FACTOR = 100;

    static int add(int a, int b) {
        return a + b;
    }

    static int meterToCm(int meter) {
        return meter * FACTOR;
    }
}

class StaticMethodExample implements UnitConverter {

    void test() {

        // FACTOR can be accessed directly
        System.out.println(FACTOR);

        // Static methods still require interface name
        System.out.println(UnitConverter.add(7, 8));
        System.out.println(UnitConverter.meterToCm(2));
    }
}

/*
 * Default methods were introduced mainly to support interface evolution.
 * They allow adding new methods to an interface without breaking
 * existing implementations.
 */
interface DefaultPrinter {

    default void print() {
        System.out.println("Printing using default implementation");
    }
}

class BasicPrinter implements DefaultPrinter {
    // uses default implementation
}

class CustomPrinter implements DefaultPrinter {

    @Override
    public void print() {
        System.out.println("Custom printer implementation");
    }
}