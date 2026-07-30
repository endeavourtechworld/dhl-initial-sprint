package com.dhl.account.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<String, TokenBucket> ipBuckets = new ConcurrentHashMap<>();
    
    // Limits: Max 5 requests, refilling 1 token every 10 seconds (max 6 requests per minute)
    private static final int BUCKET_CAPACITY = 5;
    private static final long REFILL_PERIOD_MS = 10000; 

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = getClientIp(request);
        TokenBucket bucket = ipBuckets.computeIfAbsent(ip, k -> new TokenBucket(BUCKET_CAPACITY, REFILL_PERIOD_MS));

        if (!bucket.tryConsume()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Too many requests. Please try again in a few moments.\"}");
            return false;
        }
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    private static class TokenBucket {
        private final int capacity;
        private final long refillPeriodMs;
        private double tokens;
        private long lastRefillTime;

        public TokenBucket(int capacity, long refillPeriodMs) {
            this.capacity = capacity;
            this.refillPeriodMs = refillPeriodMs;
            this.tokens = capacity;
            this.lastRefillTime = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsedTime = now - lastRefillTime;
            if (elapsedTime > 0) {
                double tokensToAdd = (double) elapsedTime / refillPeriodMs;
                tokens = Math.min(capacity, tokens + tokensToAdd);
                lastRefillTime = now;
            }
        }
    }
}
