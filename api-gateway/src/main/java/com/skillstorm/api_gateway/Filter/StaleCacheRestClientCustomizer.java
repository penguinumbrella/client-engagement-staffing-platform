package com.skillstorm.api_gateway.Filter;

import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/*
 * Registers StaleCacheClientHttpRequestInterceptor on the RestClient Spring
 * Cloud Gateway MVC uses for its downstream proxy calls (see
 * RestClientProxyExchange) — this is the standard Spring Boot extension
 * point for customizing an auto-configured RestClient.Builder.
 */
@Component
public class StaleCacheRestClientCustomizer implements RestClientCustomizer {

    private final CacheManager cacheManager;

    public StaleCacheRestClientCustomizer(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void customize(RestClient.Builder restClientBuilder) {
        restClientBuilder.requestInterceptor(new StaleCacheClientHttpRequestInterceptor(cacheManager));
    }
}
