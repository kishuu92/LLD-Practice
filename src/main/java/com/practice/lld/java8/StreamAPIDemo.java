package com.practice.lld.java8;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

/**
 * Java Stream API - Quick Revision + Interview Notes
 *
 * A Stream is a sequence of elements supporting functional-style operations.
 *
 * =========================
 * WHY STREAMS?
 * =========================
 * - Declarative style (what to do, not how)
 * - Less boilerplate
 * - Parallel processing support
 *
 * =========================
 * STREAM PIPELINE
 * =========================
 * Source -> Intermediate Ops -> Terminal Ops
 *
 * Intermediate (lazy):
 *  - filter(Predicate)
 *  - map(Function)
 *  - flatMap(Function)  -> map + flatten
 *  - distinct(), sorted(), peek()
 *
 * Terminal (eager):
 *  - collect()
 *  - forEach()
 *  - reduce()
 *  - min(), max()
 *  - findFirst(), anyMatch(), allMatch()
 *
 * =========================
 * IMPORTANT NOTES
 * =========================
 * 1. Streams are LAZY (no execution until terminal op)
 * 2. Streams are NOT reusable
 * 3. `.toList()` (Java 16+) returns IMMUTABLE list
 * 4. `Collectors.toList()` returns MUTABLE list
 * 5. Parallel streams are not always faster (use carefully)
 *
 */
public class StreamAPIDemo {

    public static void main(String[] args) {

        filterAndMapUsers();
        filterMapSortNumbers();
        sumWithFilter();
        findLongestString();
        characterFrequency();
        flattenNestedList();
        parallelSumExample();
        firstNonRepeatedCharacter();
        joinStrings();
        factorialUsingReduce();
        checkAllMatch();
        removeDuplicateCharacters();
        mostFrequentCharacter();
    }

    /**
     * Filter + map example
     */
    static void filterAndMapUsers() {
        List<User> users = List.of(
                new User(1, "Alice"),
                new User(2, "Bob"),
                new User(3, "Charlie"),
                new User(4, "David")
        );

        List<String> names = users.stream()
                .filter(user -> user.getId() > 2)
                .map(User::getName)
                .map(String::toUpperCase)
                .toList();

        System.out.println(names);
    }

    /**
     * Even numbers -> square -> sort
     */
    static void filterMapSortNumbers() {

        List<Integer> list = List.of(1,4,5,7,13,7,23,4,6,9,15,12,2);

        List<Integer> result = list.stream()
                .filter(n -> n % 2 == 0)
                .map(n -> n * n)
                .sorted()
                .toList();

        System.out.println(result);

        // Reverse sort
        result = IntStream.rangeClosed(1, 10)
                .filter(n -> n % 2 == 0)
                .map(n -> n * n)
                .boxed()
                .sorted(Comparator.reverseOrder())
                .toList();

        System.out.println(result);
    }

    /**
     * Sum of numbers < 10
     */
    static void sumWithFilter() {

        int[] arr = {2, 7, 14, 75, 3, 1, 78};

        int sum = Arrays.stream(arr)
                .filter(n -> n < 10)
                .sum();

        System.out.println(sum);
    }

    /**
     * Longest string using Comparator
     */
    static void findLongestString() {

        List<String> list = List.of("Abhishek", "Prashant", "Harishchandra");

        String result = list.stream()
                .max(Comparator.comparingInt(String::length))
                .orElse("Empty list");

        System.out.println(result);
    }

    /**
     * Character frequency using groupingBy
     */
    static void characterFrequency() {

        String str = "HelloHiHello";

        Map<Character, Long> freq = str.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()
                ));

        System.out.println(freq);
    }

    /**
     * Flatten List<List<T>> → List<T>
     */
    static void flattenNestedList() {

        List<List<String>> list = List.of(
                List.of("Hi", "Hello"),
                List.of("Bye"),
                List.of("Good Morning", "Good Night")
        );

        List<String> flat = list.stream()
                .flatMap(Collection::stream)
                .toList();

        System.out.println(flat);
    }

    /**
     * Parallel stream example
     * NOTE: Not always faster (thread overhead)
     */
    static void parallelSumExample() {

        long sum = LongStream.rangeClosed(1, 1_000_000)
                .parallel()
                .sum();

        System.out.println(sum);
    }

    /**
     * First non-repeated character (OPTIMIZED)
     * O(n) instead of O(n^2)
     */
    static void firstNonRepeatedCharacter() {

        String str = "HelloHiHello";

        Map<Character, Long> freq = str.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        Character result = freq.entrySet().stream()
                .filter(e -> e.getValue() == 1)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        System.out.println(result);
    }

    /**
     * Join strings with delimiter
     */
    static void joinStrings() {

        String[] words = {"abhishek", "kumar", "gupta"};

        String result = Arrays.stream(words)
                .collect(Collectors.joining(" - "));

        System.out.println(result);
    }

    /**
     * Factorial using reduce
     */
    static void factorialUsingReduce() {

        int n = 6;

        int fact = IntStream.rangeClosed(1, n)
                .reduce(1, (a, b) -> a * b);

        System.out.println(fact);
    }

    /**
     * Check if all elements match condition
     */
    static void checkAllMatch() {

        int[] arr = {4, 5, 7, 8, 3};

        boolean allMatch = Arrays.stream(arr)
                .allMatch(n -> n > 2);

        System.out.println(allMatch);
    }

    /**
     * removeDuplicateCharacters: removes duplicate characters from a string while preserving order
     */
    static void removeDuplicateCharacters() {

        String input = "HelloHi";

        String result = input.chars()
                .distinct()
                .collect(StringBuilder::new,
                        StringBuilder::appendCodePoint,
                        StringBuilder::append)
                .toString();

        System.out.println(result);
    }

    /**
     * mostFrequentCharacter: finds the character with highest frequency in a string
     */
    static void mostFrequentCharacter() {

        String input = "HelloHiHello";

        Map<Character, Long> freq = input.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()
                ));

        char mostFreq = Collections.max(
                freq.entrySet(),
                Map.Entry.comparingByValue()
        ).getKey();

        System.out.println(mostFreq);
    }
}

/**
 * Simple POJO
 */
@AllArgsConstructor
@Getter
@Setter
class User {
    private int id;
    private String name;
}