package com.skillstorm.engagement.client;

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

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthClientTest {

    @Mock
    private LoadBalancerClient loadBalancerClient;

    private AuthClient authClient;
    private HttpServer server;

    @BeforeEach
    void setUp() {
        authClient = new AuthClient(RestClient.builder(), loadBalancerClient, new PassthroughCircuitBreakerFactory());
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
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

    @Test
    void getUsersByRole_returnsUsersWhenAuthServiceResponds() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/users", exchange -> {
            byte[] body = ("[{\"id\":\"11111111-1111-1111-1111-111111111111\","
                    + "\"firstName\":\"Jane\",\"lastName\":\"Doe\","
                    + "\"email\":\"jane.doe@example.com\",\"role\":\"ENGAGEMENT_MANAGER\","
                    + "\"enabled\":true}]").getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        ServiceInstance instance = new DefaultServiceInstance(
                "auth-service-1", "auth-service", "localhost", server.getAddress().getPort(), false);
        when(loadBalancerClient.choose(eq("auth-service"))).thenReturn(instance);

        assertThat(authClient.getUsersByRole("ENGAGEMENT_MANAGER", "test-token")).hasSize(1);
    }

    @Test
    void getUsersByRole_returnsEmptyListInsteadOfThrowingWhenNoInstanceAvailable() {
        when(loadBalancerClient.choose(eq("auth-service"))).thenReturn(null);

        assertThatCode(() -> {
            var result = authClient.getUsersByRole("ENGAGEMENT_MANAGER", "test-token");
            assertThat(result).isEmpty();
        }).doesNotThrowAnyException();
    }

    @Test
    void getUsersByRole_returnsEmptyListInsteadOfThrowingWhenCallFails() {
        ServiceInstance instance = new DefaultServiceInstance("auth-service-1", "auth-service", "localhost", 1, false);
        when(loadBalancerClient.choose(eq("auth-service"))).thenReturn(instance);

        assertThatCode(() -> {
            var result = authClient.getUsersByRole("ENGAGEMENT_MANAGER", "test-token");
            assertThat(result).isEmpty();
        }).doesNotThrowAnyException();
    }
}
