package com.skillstorm.api_gateway.Controller;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.skillstorm.api_gateway.Filter.StaleCacheSupport;

import jakarta.servlet.RequestDispatcher;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackControllerTest {

    private CacheManager cacheManager;
    private FallbackController controller;

    @BeforeEach
    void setUp() {
        CaffeineCacheManager manager = new CaffeineCacheManager(StaleCacheSupport.CACHE_NAME);
        manager.setCaffeine(Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(24)).maximumSize(500));
        this.cacheManager = manager;
        this.controller = new FallbackController(cacheManager);
    }

    @Test
    void servesCachedResponseWhenAvailable() {
        Cache cache = cacheManager.getCache(StaleCacheSupport.CACHE_NAME);
        String key = StaleCacheSupport.buildKey("GET", "/staffing/api/consultants", null);
        cache.put(key, new StaleCacheSupport.CachedResponse(
                "[{\"id\":1}]".getBytes(StandardCharsets.UTF_8),
                "application/json",
                Instant.now()
        ));

        MockHttpServletRequest request = forwardedRequest("/staffing/api/consultants", null);

        ResponseEntity<?> response = controller.staffing(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("X-Cache-Status")).isEqualTo("stale");
        assertThat(response.getHeaders().getFirst("X-Cache-Date")).isNotNull();
        assertThat(new String((byte[]) response.getBody(), StandardCharsets.UTF_8)).isEqualTo("[{\"id\":1}]");
    }

    @Test
    void fallsBackTo503WhenCacheMiss() {
        MockHttpServletRequest request = forwardedRequest("/staffing/api/consultants", null);

        ResponseEntity<?> response = controller.staffing(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getHeaders().getFirst("X-Cache-Status")).isNull();
    }

    @Test
    void fallsBackTo503WhenDifferentQueryParams() {
        Cache cache = cacheManager.getCache(StaleCacheSupport.CACHE_NAME);
        cache.put(
                StaleCacheSupport.buildKey("GET", "/client/clients", "page=0&size=100"),
                new StaleCacheSupport.CachedResponse("[]".getBytes(StandardCharsets.UTF_8), "application/json", Instant.now())
        );

        MockHttpServletRequest request = forwardedRequest("/client/clients", "page=1&size=100");

        ResponseEntity<?> response = controller.client(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    private MockHttpServletRequest forwardedRequest(String originalPath, String originalQuery) {
        // Mirrors what the servlet container sets on a `forward:` dispatch (which is how the
        // CircuitBreaker filter's fallbackUri reaches this controller), so FallbackController
        // can recover the client's original request instead of seeing /fallback/<service>.
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/fallback/" + originalPath.split("/")[1]);
        request.setAttribute(RequestDispatcher.FORWARD_REQUEST_URI, originalPath);
        if (originalQuery != null) {
            request.setAttribute(RequestDispatcher.FORWARD_QUERY_STRING, originalQuery);
        }
        return request;
    }
}
