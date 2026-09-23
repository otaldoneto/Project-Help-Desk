package com.serviceOrder.Management.services;

import com.serviceOrder.Management.controllers.exceptions.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

// In-memory brute-force protection for /auth/login, keyed by client IP.
// Works for a single application instance; a multi-instance deployment behind a load
// balancer would need a shared store (e.g. Redis) instead, since each instance would
// otherwise keep its own separate counters.
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, Attempts> attemptsByIp = new ConcurrentHashMap<>();

    private record Attempts(int count, Instant windowStart) {
    }

    public void checkAllowed(String clientIp) {
        Attempts attempts = attemptsByIp.get(clientIp);
        if (attempts == null || windowHasExpired(attempts)) {
            return;
        }
        if (attempts.count() >= MAX_ATTEMPTS) {
            long retryAfterSeconds = Math.max(1,
                    Duration.between(Instant.now(), attempts.windowStart().plus(WINDOW)).toSeconds());
            throw new TooManyRequestsException(
                    "Too many login attempts. Try again in " + retryAfterSeconds + " seconds.", retryAfterSeconds);
        }
    }

    public void recordFailure(String clientIp) {
        attemptsByIp.compute(clientIp, (ip, current) -> {
            if (current == null || windowHasExpired(current)) {
                return new Attempts(1, Instant.now());
            }
            return new Attempts(current.count() + 1, current.windowStart());
        });
    }

    public void recordSuccess(String clientIp) {
        attemptsByIp.remove(clientIp);
    }

    private boolean windowHasExpired(Attempts attempts) {
        return Instant.now().isAfter(attempts.windowStart().plus(WINDOW));
    }
}