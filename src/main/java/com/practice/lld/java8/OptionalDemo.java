package com.practice.lld.java8;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Demonstrates usage of Optional introduced in Java 8.
 * <p>
 * Optional is a container object that may or may not contain a value.
 * It is mainly used to reduce the chances of NullPointerException
 * and to explicitly represent the absence of a value.
 * <p>
 * Common Optional factory methods:
 * <p>
 * 1. of(value)
 * Creates Optional with a non-null value.
 * Throws NullPointerException if value is null.
 * <p>
 * 2. ofNullable(value)
 * Creates Optional that may hold null.
 * If value is null, it returns Optional.empty().
 * <p>
 * 3. empty()
 * Returns an empty Optional.
 * <p>
 * Common Optional operations:
 * <p>
 * 4. ifPresent()      -> execute logic if value exists
 * 5. orElse()         -> return default value if empty (default computed eagerly)
 * 6. orElseGet()      -> lazily compute default value using Supplier
 * 7. orElseThrow()    -> throw exception if empty
 * 8. map()            -> transform value if present
 */
public class OptionalDemo {

    public static void main(String[] args) {

        UserRepository repository = new UserRepository();

        // Value present
        Optional<User> user1 = repository.findById(2);
        user1.ifPresent(user -> System.out.println("User found: " + user.getName()));

        // Default value using orElse
        // NOTE: The default object is created EVEN IF Optional contains a value
        // because method arguments are evaluated before the method call.
        User user2 = repository.findById(10)
                .orElse(new User(0, "Default User"));
        System.out.println("User: " + user2.getName());

        // Default value using orElseGet
        // The Supplier is executed ONLY if Optional is empty
        // so the default object is created lazily.
        User user3 = repository.findById(10)
                .orElseGet(() -> new User(0, "Default User"));
        System.out.println("User: " + user3.getName());

        // Transform value using map()
        String name = repository.findById(3)
                .map(User::getName)
                .orElse("Unknown");
        System.out.println("Name: " + name);

        // Using ofNullable()
        // Useful when a value might be null.
        String email = getEmailFromExternalService();

        Optional<String> emailOptional = Optional.ofNullable(email);

        String result = emailOptional
                .map(e -> "Email: " + e)
                .orElse("No email available");

        System.out.println(result);

        // Throw exception if not found
        repository.findById(20)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Simulates a method that might return null
    private static String getEmailFromExternalService() {
        return "kishuu92@gmail.com";
    }
}

/**
 * Simple repository that returns Optional<User>.
 * Demonstrates how Optional is commonly used in service/repository layers.
 */
class UserRepository {

    private final List<User> users = Arrays.asList(
            new User(1, "Alice"),
            new User(2, "Bob"),
            new User(3, "Charlie"),
            new User(4, "David")
    );

    public Optional<User> findById(int id) {
        for (User user : users) {
            if (user.getId() == id) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }
}