package com.skillstorm.staffing.client;

import com.skillstorm.staffing.dto.AuthUserResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class AuthClient {

    private static final Logger log = LoggerFactory.getLogger(AuthClient.class);
    private static final String CIRCUIT_BREAKER_ID = "auth";

    private final RestClient restClient;
    private final LoadBalancerClient loadBalancerClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public AuthClient(
            RestClient.Builder builder,
            LoadBalancerClient loadBalancerClient,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {

        this.restClient = builder.build();
        this.loadBalancerClient = loadBalancerClient;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public AuthUserResponse getUserByEmail(String email, String token) {
        return circuitBreakerFactory.create(CIRCUIT_BREAKER_ID)
                .run(() -> doGetUserByEmail(email, token), this::fallback);
    }

    private AuthUserResponse doGetUserByEmail(String email, String token) {

        ServiceInstance instance =
                loadBalancerClient.choose("auth-service");

        if (instance == null) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Auth service is not available"
                );
        }

        return restClient
                .get()
                .uri(
                        instance.getUri()
                                + "/api/users/by-email?email={email}",
                        email
                )
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .retrieve()
                .body(AuthUserResponse.class);
    }

    /*
     * Runs when the underlying call fails (including a call timeout)
     * or the breaker is open and short-circuiting calls.
     */
    private AuthUserResponse fallback(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            throw rse;
        }

        log.warn("Auth service call failed or circuit is open: {}", ex.getMessage());
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Unable to reach auth service: " + ex.getMessage()
        );
    }

    public List<AuthUserResponse> getUsersByRole(String role, String token) {

        ServiceInstance instance =
                loadBalancerClient.choose("auth-service");

        if (instance == null) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Auth service is not available"
                );
        }

        List<AuthUserResponse> users =
                restClient
                        .get()
                        .uri(
                                instance.getUri()
                                        + "/api/users?role={role}",
                                role
                        )
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
                        .retrieve()
                        .body(
                                new ParameterizedTypeReference<List<AuthUserResponse>>() {
                                }
                        );

        return users != null ? users : List.of();
    }
}