package com.skillstorm.api_gateway.Filter;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.sun.net.httpserver.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OauthAuthorizationProxyFilterTest {

    @Mock
    private LoadBalancerClient loadBalancer;

    private OauthAuthorizationProxyFilter filter;
    private HttpServer server;

    @BeforeEach
    void setUp() throws Exception {
        filter = new OauthAuthorizationProxyFilter(loadBalancer);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/oauth2/authorization/google", exchange -> {
            exchange.getResponseHeaders().add("Location", "https://accounts.google.com/o/oauth2/v2/auth?client_id=test");
            exchange.getResponseHeaders().add("Set-Cookie", "oauth_state=abc; Path=/; HttpOnly");
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
    void returnsAuthServiceRedirectWithoutFollowingIt() throws Exception {
        int port = server.getAddress().getPort();
        when(loadBalancer.choose(OauthAuthorizationProxyFilter.AUTH_SERVICE_ID))
                .thenReturn(new DefaultServiceInstance(
                        "auth-1",
                        OauthAuthorizationProxyFilter.AUTH_SERVICE_ID,
                        "127.0.0.1",
                        port,
                        false));

        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/auth/oauth2/authorization/google");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getHeader("Location"))
                .isEqualTo("https://accounts.google.com/o/oauth2/v2/auth?client_id=test");
        assertThat(response.getHeader("Set-Cookie")).contains("oauth_state=abc");
        assertThat(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8))
                .doesNotContain("followed");
    }

    @Test
    void ignoresNonOauthPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
        verifyNoInteractions(loadBalancer);
    }
}
