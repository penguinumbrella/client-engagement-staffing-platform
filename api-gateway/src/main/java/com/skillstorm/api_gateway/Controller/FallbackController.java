package com.skillstorm.api_gateway.Controller;

import java.util.Map;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.api_gateway.Filter.StaleCacheSupport;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Hit via the CircuitBreaker filter's fallbackUri on each gateway route when
 * the downstream service fails to respond (timeout, connection refused, or
 * the breaker is already open and short-circuiting). If
 * StaleCacheRestClientCustomizer captured a successful response for this
 * same request before the service went down, that's served instead — 200
 * OK, marked stale via response headers. Otherwise falls back to a 503
 * naming the specific service that's down instead of letting the failure
 * surface as a generic 500/timeout.
 *
 * Response shape on a cache miss matches the {"message": "..."} ErrorResponse
 * used by the downstream services themselves, so the frontend can read
 * err.error.message the same way regardless of whether the gateway or the
 * service produced it.
 */
@RestController
public class FallbackController {

    private final CacheManager cacheManager;

    public FallbackController(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @RequestMapping(value = "/fallback/client", method = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE })
    public ResponseEntity<?> client(HttpServletRequest request) {
        return staleOrUnavailable(request, "client");
    }

    @RequestMapping(value = "/fallback/engagement", method = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE })
    public ResponseEntity<?> engagement(HttpServletRequest request) {
        return staleOrUnavailable(request, "engagement");
    }

    @RequestMapping(value = "/fallback/staffing", method = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE })
    public ResponseEntity<?> staffing(HttpServletRequest request) {
        return staleOrUnavailable(request, "staffing");
    }

    @RequestMapping(value = "/fallback/notification", method = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE })
    public ResponseEntity<?> notification() {
        return unavailable("notification");
    }

    @RequestMapping(value = "/fallback/auth", method = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE })
    public ResponseEntity<?> auth() {
        return unavailable("auth");
    }

    private ResponseEntity<?> staleOrUnavailable(HttpServletRequest request, String service) {
        StaleCacheSupport.CachedResponse cached = lookupCache(request);

        if (cached != null) {
            MediaType contentType = cached.contentType() != null
                    ? MediaType.parseMediaType(cached.contentType())
                    : MediaType.APPLICATION_JSON;

            return ResponseEntity.ok()
                    .header("X-Cache-Status", "stale")
                    .header("X-Cache-Date", cached.cachedAt().toString())
                    .contentType(contentType)
                    .body(cached.body());
        }

        return unavailable(service);
    }

    private StaleCacheSupport.CachedResponse lookupCache(HttpServletRequest request) {
        Cache cache = cacheManager.getCache(StaleCacheSupport.CACHE_NAME);
        if (cache == null) {
            return null;
        }

        Object originalUri = request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
        if (originalUri == null) {
            return null;
        }

        Object originalQuery = request.getAttribute(RequestDispatcher.FORWARD_QUERY_STRING);

        String key = StaleCacheSupport.buildKey(
                request.getMethod(),
                originalUri.toString(),
                originalQuery != null ? originalQuery.toString() : null
        );

        Cache.ValueWrapper wrapper = cache.get(key);
        return wrapper != null ? (StaleCacheSupport.CachedResponse) wrapper.get() : null;
    }

    private ResponseEntity<Map<String, String>> unavailable(String service) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "The " + service + " service is currently unavailable. Please try again later."));
    }
}
