package com.skillstorm.staffing.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.ConfigBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetSocketAddress;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EngagementClientTest {

    @Mock
    private LoadBalancerClient loadBalancerClient;

    private EngagementClient engagementClient;
    private HttpServer server;

    @BeforeEach
    void setUp() {
        engagementClient = new EngagementClient(RestClient.builder(), loadBalancerClient, new PassthroughCircuitBreakerFactory());
    }

    /**
     * Runs the supplier directly and routes any exception straight to the
     * fallback, with no sliding-window/open-state logic of its own. Good
     * enough for exercising a client's own call/fallback wiring without
     * pulling in resilience4j's registries just to build one.
     */
    private static final class PassthroughCircuitBreakerFactory extends CircuitBreakerFactory<Object, ConfigBuilder<Object>> {
        @Override
        public CircuitBreaker create(String id) {
            return new CircuitBreaker() {
                @Override
                public <T> T run(Supplier<T> toRun, Function<Throwable, T> fallback) {
                    try {
                        return toRun.get();
                    } catch (Exception ex) {
                        return fallback.apply(ex);
                    }
                }
            };
        }

        @Override
        protected ConfigBuilder<Object> configBuilder(String id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void configureDefault(Function<String, Object> defaultConfiguration) {
            // no-op — tests don't exercise breaker tuning
        }
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void engagementExists_throwsServiceUnavailableWhenNoInstanceAvailable() {
        when(loadBalancerClient.choose(eq("engagement"))).thenReturn(null);

        assertThatThrownBy(() -> engagementClient.engagementExists(1L,"test"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Engagement service is not available");
    }

    @Test
    void engagementExists_returnsTrueWhenEngagementServiceRespondsOk() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/engagements/1", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        ServiceInstance instance = new DefaultServiceInstance(
                "engagement-1", "engagement", "localhost", server.getAddress().getPort(), false);
        when(loadBalancerClient.choose(eq("engagement"))).thenReturn(instance);

        assertThat(engagementClient.engagementExists(1L,"test")).isTrue();
    }

    @Test
    void engagementExists_returnsFalseWhenEngagementServiceRespondsNotFound() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/engagements/99", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();

        ServiceInstance instance = new DefaultServiceInstance(
                "engagement-1", "engagement", "localhost", server.getAddress().getPort(), false);
        when(loadBalancerClient.choose(eq("engagement"))).thenReturn(instance);

        assertThat(engagementClient.engagementExists(99L,"test")).isFalse();
    }

    @Test
    void engagementExists_throwsServiceUnavailableWhenCallFails() {
        ServiceInstance instance = new DefaultServiceInstance("engagement-1", "engagement", "localhost", 1, false);
        when(loadBalancerClient.choose(eq("engagement"))).thenReturn(instance);

        assertThatThrownBy(() -> engagementClient.engagementExists(1L,"test"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unable to reach engagement service");
    }
}
