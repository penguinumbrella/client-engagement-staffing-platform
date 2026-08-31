package com.skillstorm.api_gateway.Filter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/*
 * Caches successful (200) GET responses on a cacheable route so
 * FallbackController can serve them as stale data once that route's circuit
 * breaker trips.
 *
 * Gateway MVC proxies each route's downstream call on its own thread pool
 * (confirmed: RequestContextHolder isn't visible here), so the gateway-facing
 * path/query this needs for a cache key that matches what FallbackController
 * looks up can't be recovered via Spring's usual request-context propagation
 * — the downstream request this interceptor sees is already stripped/resolved
 * to the backend instance and isn't usable as a key on its own. Instead,
 * StaleCacheKeyHeaderFilter tags the request with the already-computed key
 * as a header, synchronously on the original thread, and it's read back here.
 *
 * Deliberately not a servlet Filter wrapping the response: Gateway MVC
 * streams the proxied response back to the client, and observing it via a
 * response wrapper at the servlet layer proved unreliable. Intercepting the
 * actual downstream HTTP call instead sidesteps that entirely — and since
 * FallbackController's cache hit never makes a downstream call, this
 * interceptor never fires while serving stale data, so there's no risk of
 * re-caching a stale response and resetting its TTL.
 */
public class StaleCacheClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private final CacheManager cacheManager;

    public StaleCacheClientHttpRequestInterceptor(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        String encodedKey = request.getHeaders().getFirst(StaleCacheKeyHeaderFilter.CACHE_KEY_HEADER);

        if (encodedKey == null) {
            return execution.execute(request, body);
        }

        // Strip the internal tag before it leaves the gateway — the downstream service has no use for it.
        ClientHttpResponse response = execution.execute(new HeaderRemovingRequest(request), body);

        if (response.getStatusCode() != HttpStatus.OK) {
            return response;
        }

        byte[] responseBody;
        try (InputStream in = response.getBody()) {
            responseBody = in.readAllBytes();
        }

        Cache cache = cacheManager.getCache(StaleCacheSupport.CACHE_NAME);
        if (cache != null) {
            String key = new String(Base64.getUrlDecoder().decode(encodedKey), StandardCharsets.UTF_8);
            String contentType = response.getHeaders().getContentType() != null
                    ? response.getHeaders().getContentType().toString()
                    : null;

            cache.put(key, new StaleCacheSupport.CachedResponse(responseBody, contentType, Instant.now()));
        }

        return new ReplayedClientHttpResponse(response, responseBody);
    }

    private static final class HeaderRemovingRequest implements HttpRequest {

        private final HttpRequest delegate;
        private final HttpHeaders headers;

        private HeaderRemovingRequest(HttpRequest delegate) {
            this.delegate = delegate;
            this.headers = new HttpHeaders();
            this.headers.putAll(delegate.getHeaders());
            this.headers.remove(StaleCacheKeyHeaderFilter.CACHE_KEY_HEADER);
        }

        @Override
        public HttpMethod getMethod() {
            return delegate.getMethod();
        }

        @Override
        public URI getURI() {
            return delegate.getURI();
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return delegate.getAttributes();
        }
    }

    /*
     * Reading the response body to cache it consumes the original
     * ClientHttpResponse's InputStream, so this replays those same bytes
     * for the rest of the gateway's proxying pipeline to actually stream
     * back to the client.
     */
    private static final class ReplayedClientHttpResponse implements ClientHttpResponse {

        private final ClientHttpResponse delegate;
        private final byte[] body;

        private ReplayedClientHttpResponse(ClientHttpResponse delegate, byte[] body) {
            this.delegate = delegate;
            this.body = body;
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return delegate.getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return delegate.getStatusText();
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public HttpHeaders getHeaders() {
            return delegate.getHeaders();
        }
    }
}
