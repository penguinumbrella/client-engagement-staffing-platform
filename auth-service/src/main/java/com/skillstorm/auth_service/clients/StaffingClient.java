package com.skillstorm.auth_service.clients;

import com.skillstorm.auth_service.Dtos.ProvisionConsultantRequest;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class StaffingClient {

    private static final Logger log = LoggerFactory.getLogger(StaffingClient.class);
    private static final String CIRCUIT_BREAKER_ID = "staffing";

    private final RestClient restClient;
    private final LoadBalancerClient loadBalancerClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public StaffingClient(
            RestClient.Builder builder,
            LoadBalancerClient loadBalancerClient,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {

        this.restClient = builder.build();
        this.loadBalancerClient = loadBalancerClient;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public void provisionConsultant(ProvisionConsultantRequest request, String token) {
        circuitBreakerFactory.create(CIRCUIT_BREAKER_ID)
                .run(() -> doProvisionConsultant(request, token), this::fallback);
    }

    private Void doProvisionConsultant(ProvisionConsultantRequest request, String token) {
        ServiceInstance instance = getStaffingInstance();

        restClient
                .post()
                .uri(
                        instance.getUri()
                                + "/api/consultants/provision"
                )
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .body(request)
                .retrieve()
                .toBodilessEntity();

        return null;
    }

    public void provisionConsultantByManager(
        UUID userId,
        ProvisionConsultantRequest request,
        String managerToken) {

        circuitBreakerFactory.create(CIRCUIT_BREAKER_ID)
                .run(() -> doProvisionConsultantByManager(userId, request, managerToken), this::fallback);
    }

    private Void doProvisionConsultantByManager(
        UUID userId,
        ProvisionConsultantRequest request,
        String managerToken) {

        ServiceInstance instance = getStaffingInstance();

        restClient
                .post()
                .uri(
                        instance.getUri()
                                + "/api/consultants/provision/{userId}",
                        userId
                )
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + managerToken
                )
                .body(request)
                .retrieve()
                .toBodilessEntity();

        return null;
    }

    private ServiceInstance getStaffingInstance() {

        ServiceInstance instance =
                loadBalancerClient.choose("staffing");

        if (instance == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Staffing service is not available"
            );
        }

        return instance;
    }

    /*
     * Runs when the underlying call fails (including a call timeout)
     * or the breaker is open and short-circuiting calls.
     */
    private Void fallback(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            throw rse;
        }

        log.warn("Staffing service call failed or circuit is open: {}", ex.getMessage());
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Unable to reach staffing service: " + ex.getMessage()
        );
    }
}
