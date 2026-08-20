package com.example.nova.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Simple in-memory, per-IP token-bucket rate limiter applied to the most
 * sensitive, unauthenticated endpoints (login / signup / refresh) to slow
 * down brute-force and credential-stuffing attacks.
 *
 * For a multi-instance deployment, back this with Redis (bucket4j-redis)
 * instead of the in-memory map used here.
 */
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    private static final String[] LIMITED_PATHS = {
            "/api/auth/login",
            "/api/auth/signup/individual",
            "/api/auth/signup/company",
            "/api/auth/refresh-token",
            "/api/auth/mfa/verify",
            "/api/auth/sso/exchange",
            // forgot-password: prevents mass-email abuse and slows account-existence probing
            "/api/auth/forgot-password",
            "/api/auth/reset-password"
    };

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        boolean limited = false;
        for (String p : LIMITED_PATHS) {
            if (path.equals(p)) {
                limited = true;
                break;
            }
        }

        if (!limited) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = clientKey(request);
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded, please try again later\"}");
        }
    }

    private Bucket newBucket() {
        var rateLimit = securityProperties.getRateLimit().getLogin();
        Bandwidth limit = Bandwidth.classic(rateLimit.getCapacity(),
                Refill.greedy(rateLimit.getRefillTokens(), Duration.ofMinutes(rateLimit.getRefillDurationMinutes())));
        return Bucket.builder().addLimit(limit).build();
    }

    private String clientKey(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
