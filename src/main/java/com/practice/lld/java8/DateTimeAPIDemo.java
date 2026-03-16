package com.practice.lld.java8;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * Demonstrates the Java 8 Date-Time API (java.time package).
 *
 * The old date-time classes (java.util.Date, Calendar, SimpleDateFormat)
 * had several problems:
 *
 * 1. Mutable objects (not thread-safe)
 * 2. Confusing API design
 * 3. Poor timezone handling
 * 4. Difficult date manipulation
 *
 * Java 8 introduced a new Date-Time API inspired by the Joda-Time library
 * to provide a more clear, immutable, and thread-safe way to handle dates and times.
 *
 * Key classes:
 *
 * LocalDate       -> date only (yyyy-mm-dd)
 * LocalTime       -> time only (hh:mm:ss)
 * LocalDateTime   -> date + time
 * ZonedDateTime   -> date + time + timezone
 *
 * Utility classes:
 *
 * Duration        -> time-based amount (hours, minutes)
 * Period          -> date-based amount (days, months, years)
 * DateTimeFormatter -> formatting/parsing dates
 *
 * IMPORTANT:
 * All classes in java.time are immutable.
 * Any modification returns a NEW object instead of modifying the existing one.
 */
public class DateTimeAPIDemo {

    public static void main(String[] args) {


        // LocalDate (Date only)
        LocalDate today = LocalDate.now();
        System.out.println("Today: " + today);

        LocalDate tomorrow = today.plusDays(1);
        System.out.println("Tomorrow: " + tomorrow);

        LocalDate nextMonth = today.plusMonths(1);
        System.out.println("Next Month: " + nextMonth);

        System.out.println();


        // LocalTime (Time only)
        LocalTime now = LocalTime.now();
        System.out.println("Current Time: " + now);

        LocalTime afterTwoHours = now.plusHours(2);
        System.out.println("After 2 Hours: " + afterTwoHours);

        System.out.println();


        // LocalDateTime (Date + Time)
        LocalDateTime currentDateTime = LocalDateTime.now();
        System.out.println("Current DateTime: " + currentDateTime);

        LocalDateTime yesterday = currentDateTime.minusDays(1);
        System.out.println("Yesterday same time: " + yesterday);

        System.out.println();


        // Creating specific date/time
        LocalDate birthday = LocalDate.of(1995, 5, 20);
        System.out.println("Birthday: " + birthday);

        LocalTime meetingTime = LocalTime.of(10, 30);
        System.out.println("Meeting Time: " + meetingTime);

        System.out.println();


        // Parsing String -> Date
        LocalDate parsedDate = LocalDate.parse("2026-03-20");
        System.out.println("Parsed Date: " + parsedDate);

        System.out.println();


        // Formatting Date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        String formattedDate = today.format(formatter);
        System.out.println("Formatted Date: " + formattedDate);

        System.out.println();


        // Period (difference between two dates)
        LocalDate joiningDate = LocalDate.of(2020, 1, 10);
        Period period = Period.between(joiningDate, today);

        System.out.println("Experience: "
                + period.getYears() + " years "
                + period.getMonths() + " months "
                + period.getDays() + " days");

        System.out.println();


        // Duration (time difference)
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(12, 30);

        Duration duration = Duration.between(start, end);
        System.out.println("Duration (minutes): " + duration.toMinutes());

        System.out.println();


        // Timezone handling
        ZonedDateTime indiaTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        ZonedDateTime usTime = ZonedDateTime.now(ZoneId.of("America/New_York"));

        System.out.println("India Time: " + indiaTime);
        System.out.println("US Time: " + usTime);

        System.out.println();


        // Immutability Example
        LocalDate original = LocalDate.now();
        LocalDate modified = original.plusDays(5);

        System.out.println("Original date: " + original);
        System.out.println("Modified date: " + modified);
    }
}