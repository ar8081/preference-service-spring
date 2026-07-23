package com.preferences.infrastructure.filter;

import com.preferences.presentation.exception.RateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);
    private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> windowResets = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;
    
    @Value("${preferences.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;
    
    @Value("${preferences.rate-limit.requests-per-minute:60}")
    private int globalLimit;
    
    @Value("${preferences.rate-limit.per-member-limit:10}")
    private int perMemberLimit;

    public RateLimitFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        startCleanupThread();
    }

    public void checkRateLimit(String memberId) {
        if (!rateLimitEnabled) {
            return;
        }

        long now = System.currentTimeMillis();
        long windowStart = now - 60000;

        windowResets.computeIfPresent(memberId, (key, reset) -> {
            if (reset < windowStart) {
                requestCounts.put(memberId, new AtomicInteger(0));
                return now + 60000;
            }
            return reset;
        });

        windowResets.computeIfAbsent(memberId, key -> now + 60000);
        
        AtomicInteger count = requestCounts.computeIfAbsent(memberId, key -> new AtomicInteger(0));
        int currentCount = count.incrementAndGet();

        if (currentCount > perMemberLimit) {
            meterRegistry.counter("rate_limit.exceeded", "memberId", memberId).increment();
            logger.warn("Rate limit exceeded for member: {} ({})", memberId, currentCount);
            throw new RateLimitException("Rate limit exceeded. Max " + perMemberLimit + " requests per minute");
        }

        meterRegistry.gauge("rate_limit.current", Tags.of("memberId", memberId), count, AtomicInteger::get);
    }

    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(120000);
                    long cutoff = System.currentTimeMillis() - 120000;
                    windowResets.entrySet().removeIf(entry -> entry.getValue() < cutoff);
                    requestCounts.entrySet().removeIf(entry -> {
                        Long resetTime = windowResets.get(entry.getKey());
                        return resetTime == null || resetTime < cutoff;
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }
}
