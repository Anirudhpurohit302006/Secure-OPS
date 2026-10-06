package com.secureops.security;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Minimal in-memory brute-force protection: 5 failures per client IP locks login for 5 minutes. */
@Component
public class LoginThrottle {
    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK = Duration.ofMinutes(5);

    private record State(int failures, Instant lockedUntil) {}

    private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();

    public boolean isLocked(String client) {
        State s = states.get(client);
        if (s == null || s.lockedUntil() == null) return false;
        if (Instant.now().isAfter(s.lockedUntil())) {
            states.remove(client);
            return false;
        }
        return true;
    }

    public void recordFailure(String client) {
        states.merge(client, new State(1, null), (old, ignored) -> {
            int n = old.failures() + 1;
            return new State(n, n >= MAX_FAILURES ? Instant.now().plus(LOCK) : null);
        });
    }

    public void reset(String client) { states.remove(client); }
}
