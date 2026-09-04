package com.skillstorm.api_gateway.Config;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

import com.sun.net.httpserver.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;

class HttpClientConfigTest {

    private HttpServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/from", exchange -> {
            exchange.getResponseHeaders().add("Location", "http://127.0.0.1:" + server.getAddress().getPort() + "/to");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/to", exchange -> {
            byte[] body = "followed".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void gatewayHttpClientDoesNotFollowRedirects() throws Exception {
        ClientHttpRequestFactory factory = new HttpClientConfig().clientHttpRequestFactory();
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/from";

        ClientHttpRequest request = factory.createRequest(java.net.URI.create(url), HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertThat(response.getStatusCode().value()).isEqualTo(302);
            assertThat(response.getHeaders().getFirst("Location")).endsWith("/to");
        }
    }
}
