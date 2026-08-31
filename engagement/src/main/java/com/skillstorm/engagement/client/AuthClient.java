package com.skillstorm.engagement.client;

import com.skillstorm.engagement.dto.AuthUserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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

    /**
     * Used only to resolve who to broadcast an engagement-manager
     * notification to — best-effort, like StaffingClient's staffed-consultant
     * lookup. A create/update/delete/cancel that already persisted its
     * change shouldn't fail (and appear to the caller as if nothing
     * happened) just because auth is unreachable — or just slow — when
     * it's time to notify the other EMs; skip the broadcast instead.
     *
     * Routed through the circuit breaker rather than a bare try/catch: an
     * unguarded call has no timeout of its own, so an auth instance that's
     * up but merely slow could previously stall this call — and the whole
     * engagement request it's part of — well past the gateway's own
     * timeout, turning "slow" into a false "service unavailable" for the
     * caller.
     */
    public List<AuthUserResponse> getUsersByRole(String role, String token) {

        ServiceInstance instance =
                loadBalancerClient.choose("auth-service");

        if (instance == null) {
            log.warn(
                    "Auth service is not available; skipped engagement-manager broadcast lookup for role={}",
                    role
            );
            return List.of();
        }

        return circuitBreakerFactory.create(CIRCUIT_BREAKER_ID)
                .run(
                        () -> doGetUsersByRole(instance, role, token),
                        ex -> {
                            log.warn(
                                    "Failed to fetch users with role={} for engagement-manager broadcast: {}",
                                    role,
                                    ex.getMessage()
                            );
                            return List.of();
                        }
                );
    }

    private List<AuthUserResponse> doGetUsersByRole(ServiceInstance instance, String role, String token) {

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
