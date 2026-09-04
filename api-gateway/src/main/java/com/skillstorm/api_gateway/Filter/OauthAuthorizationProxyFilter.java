package com.skillstorm.api_gateway.Filter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Set;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/*
 * Spring Cloud Gateway MVC's RestClient follows auth-service's 302 to
 * Google and returns accounts.google.com HTML from our CloudFront origin.
 * Google's sign-in scripts then fail CORS, so Next does nothing.
 *
 * This filter proxies the OAuth dance itself with follow-redirects disabled,
 * so the browser actually navigates to Google.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class OauthAuthorizationProxyFilter extends OncePerRequestFilter {

    static final String AUTH_SERVICE_ID = "auth-service";

    private static final Set<String> HOP_BY_HOP = Set.of(
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade",
            "host",
            "content-length"
    );

    private final LoadBalancerClient loadBalancer;

    public OauthAuthorizationProxyFilter(LoadBalancerClient loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean oauthPath = path.startsWith("/auth/oauth2/")
                || path.startsWith("/auth/login/oauth2/");
        boolean supportedMethod = "GET".equalsIgnoreCase(request.getMethod())
                || "HEAD".equalsIgnoreCase(request.getMethod());
        return !oauthPath || !supportedMethod;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        ServiceInstance instance = loadBalancer.choose(AUTH_SERVICE_ID);
        if (instance == null) {
            response.sendError(
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    "auth-service is not available");
            return;
        }

        String backendPath = request.getRequestURI().substring("/auth".length());
        String query = request.getQueryString();
        URI backendUri = URI.create(instance.getUri() + backendPath
                + (query == null || query.isBlank() ? "" : "?" + query));

        HttpURLConnection connection = (HttpURLConnection) backendUri.toURL().openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(5_000);
        connection.setReadTimeout(15_000);
        connection.setUseCaches(false);
        connection.setRequestMethod(request.getMethod());

        copyRequestHeaders(request, connection);

        int status = connection.getResponseCode();
        response.setStatus(status);

        copyResponseHeaders(connection, response);

        InputStream bodyStream = status >= 400
                ? connection.getErrorStream()
                : connection.getInputStream();
        if (bodyStream != null) {
            try (InputStream in = bodyStream; OutputStream out = response.getOutputStream()) {
                in.transferTo(out);
            }
        }
    }

    private static void copyRequestHeaders(
            HttpServletRequest request,
            HttpURLConnection connection) {
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (HOP_BY_HOP.contains(name.toLowerCase(Locale.ROOT))) {
                continue;
            }
            Enumeration<String> values = request.getHeaders(name);
            while (values.hasMoreElements()) {
                connection.addRequestProperty(name, values.nextElement());
            }
        }
    }

    private static void copyResponseHeaders(
            HttpURLConnection connection,
            HttpServletResponse response) {
        connection.getHeaderFields().forEach((name, values) -> {
            if (name == null || values == null) {
                return;
            }
            if (HOP_BY_HOP.contains(name.toLowerCase(Locale.ROOT))) {
                return;
            }
            for (String value : values) {
                response.addHeader(name, value);
            }
        });
    }
}
