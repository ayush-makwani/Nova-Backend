package com.example.nova.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks nonces embedded in SSO exchange codes so a code can be redeemed at
 * most once, even though it is still cryptographically valid until it
 * expires (e.g. if it leaks via browser history or a proxy access log, or
 * the frontend accidentally submits it twice).
 *
 * In-memory and per-instance, which is acceptable given codes live for only
 * 60 seconds. For a multi-instance deployment, back this with a shared cache
 * (Redis, etc.) instead - same caveat documented on RateLimitingFilter.
 */
@Component
public class SsoNonceStore {

    private final Map<String, Instant> consumed = new ConcurrentHashMap<>();

    /**
     * @return true the first time a given nonce is seen (and records it as used),
     *         false if it has already been redeemed.
     */
    public synchronized boolean consume(String nonce) {
        cleanupExpired();
        if (consumed.containsKey(nonce)) {
            return false;
        }
        consumed.put(nonce, Instant.now());
        return true;
    }

    private void cleanupExpired() {
        Instant cutoff = Instant.now().minus(5, ChronoUnit.MINUTES);
        consumed.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
    }
}
