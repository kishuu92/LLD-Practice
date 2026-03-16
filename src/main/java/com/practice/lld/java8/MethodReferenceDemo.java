package com.practice.lld.java8;

/*
 * Method References (Java 8)
 *
 * A method reference is a shorter way to refer to an existing method
 * when a lambda expression only calls that method.
 *
 * Instead of:
 * name -> System.out.println(name)
 *
 * We can write:
 * System.out::println
 *
 * Types of method references:
 * 1. Static method reference        -> ClassName::staticMethod
 * 2. Instance method reference      -> object::instanceMethod
 * 3. Constructor reference          -> ClassName::new
 */
public class MethodReferenceDemo {

    public static void main(String[] args) {

        // reference to an instance method of an existing object
        MessagePrinter printer = System.out::println;
        printer.print("Alice");

        System.out.println();


        // reference to a static method
        printer = StaticGreeter::greet;
        printer.print("Bob");

        System.out.println();


        // reference to an instance method of a particular object
        InstanceGreeter greeter = new InstanceGreeter();
        printer = greeter::greet;
        printer.print("Charlie");

        System.out.println();


        // reference to a static method with return value
        MathOperation2 adder = Calculator::add;
        System.out.println(adder.apply(7, 5));

        System.out.println();


        // constructor reference
        NameFactory factory = Person::new;
        Person person = factory.create("David");
        System.out.println(person.name);
    }
}


/*
 * Functional interface used for printing messages.
 */
interface MessagePrinter {
    void print(String message);
}


/*
 * Static method example.
 */
class StaticGreeter {

    static void greet(String name) {
        System.out.println("Hi " + name);
    }
}


/*
 * Instance method example.
 */
class InstanceGreeter {

    void greet(String name) {
        System.out.println("Hello " + name);
    }
}


/*
 * Functional interface representing a math operation.
 */
interface MathOperation2 {
    int apply(int a, int b);
}


/*
 * Utility class containing static math methods.
 */
class Calculator {

    static int add(int a, int b) {
        return a + b;
    }
}


/*
 * Constructor reference example.
 */
interface NameFactory {
    Person create(String name);
}


class Person {

    String name;

    Person(String name) {
        this.name = name;
    }
}