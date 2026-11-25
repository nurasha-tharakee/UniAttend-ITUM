package com.university.attendance.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window rate limiter for student registration and verification attempts.
 */
@Service
public class VerificationRateLimiterService {

    private static final int MAX_ATTEMPTS = 8;
    private static final long WINDOW_SECONDS = 600; // 10 minutes

    private final Map<String, Deque<Instant>> attemptsByKey = new ConcurrentHashMap<>();

    public synchronized boolean allowAttempt(String key) {
        if (key == null || key.isBlank()) {
            return true;
        }
        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(WINDOW_SECONDS);
        Deque<Instant> queue = attemptsByKey.computeIfAbsent(key, k -> new ArrayDeque<>());

        while (!queue.isEmpty() && queue.peekFirst().isBefore(cutoff)) {
            queue.pollFirst();
        }

        if (queue.size() >= MAX_ATTEMPTS) {
            return false;
        }
        queue.addLast(now);
        return true;
    }
}
