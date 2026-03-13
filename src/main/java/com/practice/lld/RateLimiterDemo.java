package com.practice.lld;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Rate Limiter Simulation
 * <p>
 * Demonstrates multiple endpoints with different rate limiters.
 * <p>
 * Highlights:
 * 1. Separate endpoints (userService, paymentService) each have their own limiter.
 * 2. Unregistered endpoints fall back to a default limiter.
 * 3. Tick-based simulation: multiple requests from same or different clients.
 * 4. Thread-safe per-client rate limiting.
 * <p>
 * Note: Sleep times simulate elapsed time between client requests.
 */

public class RateLimiterDemo {

    public static void main(String[] args) throws InterruptedException {

        // Create rate limiter and register endpoints
        RateLimiter rateLimiter = new RateLimiter();

        System.out.println("Checking for paymentService which uses sliding window");
        for (int i = 1; i <= 10; i++) {
            RateLimitResult res = rateLimiter.check("paymentService", "client1");
            System.out.println("Client1 " + i + ": " + res);
            Thread.sleep(50);
        }
        System.out.println();

        System.out.println("Checking for user services which token bucket");
        for (int i = 1; i <= 8; i++) {
            RateLimitResult res = rateLimiter.check("userService", "client1");
            System.out.println("Client1 " + i + ": " + res);
            Thread.sleep(50);
        }
        for (int i = 1; i <= 8; i++) {
            RateLimitResult res = rateLimiter.check("userService", "client1");
            System.out.println("Client1 " + i + ": " + res);
            Thread.sleep(64);
            RateLimitResult res2 = rateLimiter.check("userService", "client2");
            System.out.println("\t\tClient2 " + i + ": " + res2);
            Thread.sleep(56);
        }
        System.out.println();

        System.out.println("Checking for unregistered services, limits shared across all endpoints for given client");
        for (int i = 1; i <= 8; i++) {
            RateLimitResult res = rateLimiter.check("newService", "client1");
            System.out.println("Client1 " + i + ": " + res);
            Thread.sleep(50);
        }
        System.out.println();
        for (int i = 1; i <= 8; i++) {
            RateLimitResult res = rateLimiter.check("newService", "client1");
            System.out.println("Client1 " + i + ": " + res);
            Thread.sleep(67);
            RateLimitResult res2 = rateLimiter.check("newService2", "client1");
            System.out.println("\t\tClient1 " + i + ": " + res2);
            Thread.sleep(76);
        }
    }
}


/**
 * Core RateLimiter supporting multiple endpoints.
 * <p>
 * - Maintains a map of endpoint → Limiter.
 * - Uses default limiter for any endpoint not explicitly registered.
 * <p>
 * Note:
 * - The map reference is final, so it cannot be reassigned.
 * - Its contents are mutable (ConcurrentHashMap), allowing dynamic registration of endpoints.
 */
class RateLimiter {

    private final Map<String, Limiter> limiters;
    private final Limiter defaultLimiter;

    RateLimiter() {

        // Register known endpoints
        limiters = new ConcurrentHashMap<>(Map.of(
                "userService", new TokenBucketLimiter(5, 3, 3, Clock.systemUTC()),
                "paymentService", new SlidingWindowLimiter(2, 200, Clock.systemUTC())
        ));

        // Default limiter for any unregistered endpoint
        defaultLimiter = new TokenBucketLimiter(20, 10, 5, Clock.systemUTC());
    }

    public void registerEndpoint(String endpoint, Limiter limiter) {
        limiters.put(endpoint, limiter);
    }

    RateLimitResult check(String endPoint, String clientId) {
        Limiter limiter = limiters.getOrDefault(endPoint, defaultLimiter);
        return limiter.check(clientId);
    }

}

/**
 * Interface for rate limiting strategies.
 * <p>
 * Implementations (e.g., TokenBucketLimiter, SlidingWindowLimiter) handle per-client state
 * and enforce rate limits.
 */
interface Limiter {

    RateLimitResult check(String clientId);
}

/**
 * Token Bucket Limiter implementation.
 * <p>
 * Uses per-client TokenBucket stored in a ConcurrentHashMap.
 * - Each bucket has its own ReentrantLock to synchronize token refill and consumption.
 * <p>
 * Why ReentrantLock over synchronized:
 * 1. Can implement tryLock() or timeout-based locking if needed.
 * 2. Offers fairness policies (first-come-first-served) in highly concurrent scenarios.
 * 3. Provides better observability and debugging compared to synchronized blocks.
 * <p>
 * initialTokens allows starting with a burst capacity.
 * <p>
 * Production note:
 * - Map can grow indefinitely; in a real system, evict inactive clients using TTL/LRU or external cache.
 */
@RequiredArgsConstructor
class TokenBucketLimiter implements Limiter {

    private final int capacity;
    private final double initialTokens;
    private final int refillRatePerSecond;
    private final Clock clock;

    Map<String, TokenBucketLimiter.TokenBucket> buckets = new ConcurrentHashMap<>();

    @Override
    public RateLimitResult check(String clientId) {

        long now = clock.millis();
        TokenBucketLimiter.TokenBucket bucket = buckets.computeIfAbsent(clientId, k -> new TokenBucketLimiter.TokenBucket(initialTokens, now));

        bucket.lock.lock();
        try {
            long last = bucket.lastRefillTime;
            long elapsed = now - last;

            double tokensToAdd = elapsed / 1000.0 * refillRatePerSecond;
            bucket.tokens = Math.min(capacity, bucket.tokens + tokensToAdd);
            bucket.lastRefillTime = now;

            if (bucket.tokens >= 1) {
                bucket.tokens -= 1;
                int remaining = (int) Math.floor(bucket.tokens);
                return new RateLimitResult(true, remaining, 0);
            }
            double tokenNeeded = 1 - bucket.tokens;
            long retryAfterMs = (long) Math.ceil((tokenNeeded * 1000) / refillRatePerSecond);
            return new RateLimitResult(false, 0, retryAfterMs);
        } finally {
            bucket.lock.unlock();
        }
    }

    /**
     * Represents a per-client token bucket.
     * <p>
     * - tokens: current available tokens (can be fractional for precision).
     * - lastRefillTime: timestamp in millis of last refill.
     * - lock: ReentrantLock to safely update tokens and lastRefillTime.
     * <p>
     * Using per-bucket lock instead of global lock ensures minimal contention
     * when multiple clients access the limiter concurrently.
     */
    @AllArgsConstructor
    static class TokenBucket {

        private double tokens;
        private long lastRefillTime;
        private final ReentrantLock lock = new ReentrantLock();
    }
}

/**
 * Sliding Window Limiter implementation.
 * <p>
 * Maintains a deque of request timestamps per client.
 * <p>
 * Thread safety:
 * - Synchronized on each client’s RequestLog object.
 * - Ensures multiple threads can safely add or remove timestamps.
 * <p>
 * Production note / optimization:
 * - Using Deque for timestamps is simple but memory-heavy for high request volume.
 * - Better approach: Use a fixed-size circular bucket counter (e.g., 1-second buckets)
 * to approximate sliding window while reducing memory footprint.
 * <p>
 * Map can also grow indefinitely; consider eviction strategies for long-lived systems.
 */
@RequiredArgsConstructor
class SlidingWindowLimiter implements Limiter {

    private final int capacity;
    private final long windowMs;
    private final Clock clock;

    Map<String, SlidingWindowLimiter.RequestLog> logs = new ConcurrentHashMap<>();

    @Override
    public RateLimitResult check(String clientId) {

        SlidingWindowLimiter.RequestLog log = logs.computeIfAbsent(clientId, k -> new SlidingWindowLimiter.RequestLog());
        long now = clock.millis();
        long cutoff = now - windowMs;

        log.lock.lock();
        try {

            while (!log.timestamps.isEmpty() && log.timestamps.peekFirst() < cutoff)
                log.timestamps.pollFirst();

            if (log.timestamps.size() < capacity) {
                log.timestamps.addLast(now);
                int remaining = capacity - log.timestamps.size();
                return new RateLimitResult(true, remaining, 0);
            }

            long lastTime = log.timestamps.peekFirst();
            long retryAfterMs = lastTime + windowMs - now;
            return new RateLimitResult(false, 0, retryAfterMs);
        } finally {
            log.lock.unlock();
        }
    }

    /**
     * Holds per-client timestamps for sliding window.
     * <p>
     * Uses ArrayDeque protected by ReentrantLock.
     * - Lock ensures thread safety, so we don't need ConcurrentLinkedDeque.
     * - More memory-efficient and faster than a concurrent deque for per-client logs.
     * <p>
     * In high-throughput systems:
     * - Each timestamp is a long, so memory usage is O(requests).
     * - For very large request volume, consider circular fixed-size bucket counters
     * to approximate sliding window and reduce memory usage.
     */
    static class RequestLog {

        private final Deque<Long> timestamps = new ArrayDeque<>();
        private final ReentrantLock lock = new ReentrantLock();
    }
}

/**
 * Result of a rate limit check.
 * <p>
 * Fields:
 * - allowed: true if request can proceed.
 * - remaining: number of requests remaining in current window or token count.
 * - retryAfterMs: time to wait before next allowed request (if not allowed).
 * <p>
 * toString() provides readable output for logging/testing.
 */
@Getter
@AllArgsConstructor
class RateLimitResult {

    private final boolean allowed;
    private final int remaining;
    private final long retryAfterMs;

    @Override
    public String toString() {
        if (allowed) return "(allowed=true, remaining=" + remaining + ")";
        else return "(allowed=false, retryAfterMs=" + retryAfterMs + ")";
    }
}
