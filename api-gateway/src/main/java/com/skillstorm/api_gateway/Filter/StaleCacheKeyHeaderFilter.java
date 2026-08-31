package com.skillstorm.api_gateway.Filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/*
 * Gateway MVC proxies each route's downstream call on a separate thread
 * pool, so RequestContextHolder (thread-local) isn't visible from
 * StaleCacheClientHttpRequestInterceptor by the time it runs — it can't
 * recover which gateway-facing route/path the call belongs to on its own.
 *
 * This filter runs synchronously on the original request thread (guaranteed,
 * as a plain OncePerRequestFilter) and tags the request with the
 * already-computed cache key as a request header. Headers travel with the
 * request wherever Gateway MVC forwards it — including across that thread
 * hop — so the interceptor can read it back regardless of which thread
 * actually executes.
 */
@Component
public class StaleCacheKeyHeaderFilter extends OncePerRequestFilter {

    public static final String CACHE_KEY_HEADER = "X-Stale-Cache-Key";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String method = request.getMethod();
        String path = request.getRequestURI();

        if (!StaleCacheSupport.isCacheable(method, path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = StaleCacheSupport.buildKey(method, path, request.getQueryString());
        String encodedKey = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(key.getBytes(StandardCharsets.UTF_8));

        filterChain.doFilter(new CacheKeyHeaderRequest(request, encodedKey), response);
    }

    private static final class CacheKeyHeaderRequest extends HttpServletRequestWrapper {

        private final String encodedKey;

        private CacheKeyHeaderRequest(HttpServletRequest request, String encodedKey) {
            super(request);
            this.encodedKey = encodedKey;
        }

        @Override
        public String getHeader(String name) {
            if (CACHE_KEY_HEADER.equalsIgnoreCase(name)) {
                return encodedKey;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (CACHE_KEY_HEADER.equalsIgnoreCase(name)) {
                return Collections.enumeration(List.of(encodedKey));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = Collections.list(super.getHeaderNames());
            names.add(CACHE_KEY_HEADER);
            return Collections.enumeration(names);
        }
    }
}
