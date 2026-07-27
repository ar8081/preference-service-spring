package com.preferences.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {
    public static final String PREFERENCES_CACHE = "preferences";

    @Value("${prefernces.cache.ttl-seconds:300}")
    private long ttlSeconds;

    @Value("${preferences.cache.max-size:1000}")
    private long maxSize;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(PREFERENCES_CACHE);
        manager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                        .maximumSize(maxSize)
                        .recordStats()  // enables hit/miss metrics at /actuator/metrics
        );
        return manager;
    }
}
