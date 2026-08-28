package com.skillstorm.engagement.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.web.client.RestClient;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

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
        authClient = new AuthClient(RestClient.builder(), loadBalancerClient);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
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
