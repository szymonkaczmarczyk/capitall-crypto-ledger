package com.capitall.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitFilter.class);

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(5);
    private static final Duration RETENTION = Duration.ofMinutes(15);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(request.getMethod()) && isProtectedPath(request.getServletPath())) {
            String key = request.getServletPath() + "|" + ClientIpResolver.resolve(request);
            if (!allow(key)) {
                log.warn("Rate limit exceeded for {}", key);
                response.setStatus(429);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("Too many attempts. Please wait a few minutes and try again.");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isProtectedPath(String path) {
        return "/login".equals(path) || "/register".equals(path);
    }

    private boolean allow(String key) {
        Instant now = Instant.now();
        cleanupIfStale(now);
        Bucket bucket = buckets.compute(key, (k, existing) -> {
            if (existing == null || Duration.between(existing.windowStart, now).compareTo(WINDOW) > 0) {
                return new Bucket(now);
            }
            return existing;
        });
        return bucket.count.incrementAndGet() <= MAX_ATTEMPTS;
    }

    private volatile Instant lastCleanup = Instant.EPOCH;

    private void cleanupIfStale(Instant now) {
        if (Duration.between(lastCleanup, now).compareTo(Duration.ofMinutes(5)) < 0) return;
        lastCleanup = now;
        buckets.entrySet().removeIf(e -> Duration.between(e.getValue().windowStart, now).compareTo(RETENTION) > 0);
    }

    private static final class Bucket {
        final Instant windowStart;
        final AtomicInteger count = new AtomicInteger(0);
        Bucket(Instant start) { this.windowStart = start; }
    }
}
