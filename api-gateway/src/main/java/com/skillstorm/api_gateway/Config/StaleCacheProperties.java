package com.skillstorm.api_gateway.Config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.stale-cache")
public record StaleCacheProperties(Duration ttl, int maxEntries) {
}
