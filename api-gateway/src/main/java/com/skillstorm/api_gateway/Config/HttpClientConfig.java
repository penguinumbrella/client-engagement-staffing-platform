package com.skillstorm.api_gateway.Config;

import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

/*
 * Every route proxied by the gateway (spring-cloud-gateway-server-webmvc)
 * shares the RestClient built by GatewayServerMvcAutoConfiguration, which
 * picks up whatever ClientHttpRequestFactory bean is available here
 * (see its gatewayRestClientCustomizer). Left unconfigured, that RestClient
 * defaults to an Apache HttpClient5 connection pool sized for a handful of
 * requests (maxTotal=25, maxPerRoute=5) with no connection-request timeout
 * — once every pooled connection to a route is checked out, any further
 * call to that route blocks on StrictConnPool.get() indefinitely, only
 * ever cut off by the outer resilience4j TimeLimiter. That's what a false
 * "service unavailable" from a service that's actually up and fast looks
 * like from a thread dump: stuck acquiring a connection, not stuck talking
 * to the service. Sizing the pool well above normal concurrent traffic,
 * and giving lease acquisition its own bounded timeout, is what actually
 * fixes it instead of just giving the symptom more room to hide in.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {

        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(200)
                .setMaxConnPerRoute(50)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(Timeout.ofSeconds(5))
                        .setConnectTimeout(Timeout.ofSeconds(5))
                        .build())
                .evictIdleConnections(TimeValue.of(30, TimeUnit.SECONDS))
                .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
