package com.skillstorm.api_gateway.Filter;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/*
 * Shared by StaleCacheFilter (writes successful responses in) and
 * FallbackController (reads them back out on a circuit-breaker trip), so
 * both sides always agree on what's cacheable and how a cache key is built.
 */
public final class StaleCacheSupport {

    public static final String CACHE_NAME = "staleResponses";

    private static final List<String> CACHEABLE_PATH_PREFIXES = List.of("/client/", "/engagement/", "/staffing/");

    private StaleCacheSupport() {
    }

    public static boolean isCacheable(String method, String path) {
        if (!"GET".equalsIgnoreCase(method)) {
            return false;
        }

        boolean underCacheableRoute = CACHEABLE_PATH_PREFIXES.stream().anyMatch(path::startsWith);
        if (!underCacheableRoute) {
            return false;
        }

        return Arrays.stream(path.split("/")).noneMatch("me"::equals);
    }

    public static String buildKey(String method, String path, String queryString) {
        String normalizedQuery = normalizeQuery(queryString);
        return method.toUpperCase() + " " + path + (normalizedQuery.isEmpty() ? "" : "?" + normalizedQuery);
    }

    private static String normalizeQuery(String queryString) {
        if (queryString == null || queryString.isBlank()) {
            return "";
        }

        return Arrays.stream(queryString.split("&")).sorted().collect(Collectors.joining("&"));
    }

    public record CachedResponse(byte[] body, String contentType, Instant cachedAt) {
    }
}
