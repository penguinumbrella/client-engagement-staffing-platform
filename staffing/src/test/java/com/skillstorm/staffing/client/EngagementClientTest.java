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
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetSocketAddress;

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
        engagementClient = new EngagementClient(RestClient.builder(), loadBalancerClient);
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

        assertThatThrownBy(() -> engagementClient.engagementExists(1L))
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

        assertThat(engagementClient.engagementExists(1L)).isTrue();
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

        assertThat(engagementClient.engagementExists(99L)).isFalse();
    }

    @Test
    void engagementExists_throwsServiceUnavailableWhenCallFails() {
        ServiceInstance instance = new DefaultServiceInstance("engagement-1", "engagement", "localhost", 1, false);
        when(loadBalancerClient.choose(eq("engagement"))).thenReturn(instance);

        assertThatThrownBy(() -> engagementClient.engagementExists(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unable to reach engagement service");
    }
}
