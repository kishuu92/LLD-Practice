package com.practice.lld.java8;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Stream is a sequence of elements that supports functional-style operations to process data
 *
 * Used for - filtering, mapping, sorting, collecting, aggregating etc.
 *
 */
public class StreamAPIDemo {

    public static void main(String[] args) {

        List<User> users = Arrays.asList(new User(1, "Alice"), new User(2, "Bob"),
                new User(3, "Charlie"), new User(4, "David"));

        List<String> names = users.stream()
                .filter(k -> k.getId() > 2)
                .map(User::getName)
                .map(String::toUpperCase)
                .toList();
        System.out.println(names);

    }


}


@AllArgsConstructor
@Getter
@Setter
class User {
    int id;
    String name;
}