package com.skillstorm.api_gateway.Filter;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import com.github.benmanes.caffeine.cache.Caffeine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaleCacheClientHttpRequestInterceptorTest {

    @Mock
    private HttpRequest httpRequest;

    @Mock
    private ClientHttpRequestExecution execution;

    @Mock
    private ClientHttpResponse response;

    private CacheManager cacheManager;
    private StaleCacheClientHttpRequestInterceptor interceptor;

    @BeforeEach
    void setUp() {
        CaffeineCacheManager manager = new CaffeineCacheManager(StaleCacheSupport.CACHE_NAME);
        manager.setCaffeine(Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(24)).maximumSize(500));
        this.cacheManager = manager;
        this.interceptor = new StaleCacheClientHttpRequestInterceptor(cacheManager);
    }

    @Test
    void cachesSuccessfulGetWhenRequestTaggedWithCacheKey() throws Exception {
        tagWith("GET", "/staffing/api/consultants", null);
        when(execution.execute(any(), any())).thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);
        when(response.getBody()).thenReturn(new ByteArrayInputStream("[{\"id\":1}]".getBytes(StandardCharsets.UTF_8)));
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        when(response.getHeaders()).thenReturn(responseHeaders);

        ClientHttpResponse result = interceptor.intercept(httpRequest, new byte[0], execution);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(new String(result.getBody().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("[{\"id\":1}]");

        Cache cache = cacheManager.getCache(StaleCacheSupport.CACHE_NAME);
        String key = StaleCacheSupport.buildKey("GET", "/staffing/api/consultants", null);
        StaleCacheSupport.CachedResponse cached = (StaleCacheSupport.CachedResponse) cache.get(key).get();

        assertThat(new String(cached.body(), StandardCharsets.UTF_8)).isEqualTo("[{\"id\":1}]");
        assertThat(cached.contentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    void stripsTheTagHeaderBeforeForwardingDownstream() throws Exception {
        tagWith("GET", "/staffing/api/consultants", null);
        when(execution.execute(any(), any())).thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);
        when(response.getBody()).thenReturn(new ByteArrayInputStream("[]".getBytes(StandardCharsets.UTF_8)));
        when(response.getHeaders()).thenReturn(new HttpHeaders());

        interceptor.intercept(httpRequest, new byte[0], execution);

        var requestCaptor = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(execution).execute(requestCaptor.capture(), any());

        assertThat(requestCaptor.getValue().getHeaders().containsHeader(StaleCacheKeyHeaderFilter.CACHE_KEY_HEADER)).isFalse();
    }

    @Test
    void doesNotCacheWhenRequestIsNotTagged() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        when(httpRequest.getHeaders()).thenReturn(headers);
        when(execution.execute(any(), any())).thenReturn(response);

        ClientHttpResponse result = interceptor.intercept(httpRequest, new byte[0], execution);

        assertThat(result).isSameAs(response);
        assertThat(cacheManager.getCache(StaleCacheSupport.CACHE_NAME).get(
                StaleCacheSupport.buildKey("GET", "/staffing/api/consultants", null))).isNull();
    }

    @Test
    void doesNotCacheNonOkResponses() throws Exception {
        tagWith("GET", "/staffing/api/consultants", null);
        when(execution.execute(any(), any())).thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

        interceptor.intercept(httpRequest, new byte[0], execution);

        Cache cache = cacheManager.getCache(StaleCacheSupport.CACHE_NAME);
        String key = StaleCacheSupport.buildKey("GET", "/staffing/api/consultants", null);

        assertThat(cache.get(key)).isNull();
    }

    private void tagWith(String method, String path, String query) {
        String key = StaleCacheSupport.buildKey(method, path, query);
        String encodedKey = Base64.getUrlEncoder().withoutPadding().encodeToString(key.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set(StaleCacheKeyHeaderFilter.CACHE_KEY_HEADER, encodedKey);
        when(httpRequest.getHeaders()).thenReturn(headers);
    }
}
