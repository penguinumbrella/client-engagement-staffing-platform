package com.skillstorm.client.clients;

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
public class EngagementClient {

    private static final Logger log = LoggerFactory.getLogger(EngagementClient.class);
    private static final String CIRCUIT_BREAKER_ID = "engagement";

    private final RestClient restClient;
    private final LoadBalancerClient loadBalancerClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public EngagementClient(RestClient.Builder restClientBuilder, LoadBalancerClient loadBalancerClient,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.restClient = restClientBuilder.build();
        this.loadBalancerClient = loadBalancerClient;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public boolean hasActiveEngagements(Long clientId, String token) {
        return circuitBreakerFactory.create(CIRCUIT_BREAKER_ID)
                .run(() -> callEngagementService(clientId, token), this::fallback);
    }

    private boolean callEngagementService(Long clientId, String token) {
        ServiceInstance instance = loadBalancerClient.choose("engagement");

        if (instance == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Engagement service is not available"
            );
        }

        List<Object> engagements = restClient.get()
                .uri(
                        instance.getUri()
                                + "/api/engagements/client/{clientId}",
                        clientId
                )
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .retrieve()
                .body(new ParameterizedTypeReference<List<Object>>() {});
        return engagements != null && !engagements.isEmpty();
    }

    /*
     * Runs when the underlying call fails (including a call timeout)
     * or the breaker is open and short-circuiting calls.
     */
    private boolean fallback(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            throw rse;
        }

        log.warn("Engagement service call failed or circuit is open: {}", ex.getMessage());
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Unable to reach engagement service: " + ex.getMessage()
        );
    }
}
