package com.skillstorm.api_gateway.Config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.skillstorm.api_gateway.Filter.StaleCacheSupport;

/*
 * Backs the stale-response cache the CircuitBreaker fallback (see
 * FallbackController) reads from and StaleCacheRestClientCustomizer writes
 * to. Plain CacheManager/Cache usage rather than @Cacheable — the write
 * side always lets the live call through and only observes successful
 * responses, it never reads from the cache to answer a healthy request.
 */
@Configuration
@EnableConfigurationProperties(StaleCacheProperties.class)
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(StaleCacheProperties properties) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(StaleCacheSupport.CACHE_NAME);
        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(properties.ttl())
                        .maximumSize(properties.maxEntries())
        );
        return cacheManager;
    }
}
