package com.skillstorm.engagement.client;

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
public class StaffingClient {

    private static final Logger log =
            LoggerFactory.getLogger(StaffingClient.class);
    private static final String CIRCUIT_BREAKER_ID = "staffing";

    private final RestClient restClient;
    private final LoadBalancerClient loadBalancerClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public StaffingClient(
            RestClient.Builder restClientBuilder,
            LoadBalancerClient loadBalancerClient,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {

        this.restClient = restClientBuilder.build();
        this.loadBalancerClient = loadBalancerClient;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public List<Long> getCurrentUserEngagementIds(String token) {
        return circuitBreakerFactory.create(CIRCUIT_BREAKER_ID)
                .run(() -> doGetCurrentUserEngagementIds(token), this::fallback);
    }

    private List<Long> doGetCurrentUserEngagementIds(String token) {

        ServiceInstance instance = getStaffingInstance();

        List<Long> engagementIds =
                restClient
                        .get()
                        .uri(instance.getUri()
                                + "/api/assignments/me/engagement-ids")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
                        .retrieve()
                        .body(
                                new ParameterizedTypeReference<List<Long>>() {
                                }
                        );

        return engagementIds != null
                ? engagementIds
                : List.of();
    }

    public boolean isCurrentUserAssigned(
            Long engagementId,
            String token) {

        return circuitBreakerFactory.create(CIRCUIT_BREAKER_ID)
                .run(() -> doIsCurrentUserAssigned(engagementId, token), this::fallback);
    }

    private boolean doIsCurrentUserAssigned(
            Long engagementId,
            String token) {

        ServiceInstance instance = getStaffingInstance();

        Boolean assigned =
                restClient
                        .get()
                        .uri(
                                instance.getUri()
                                        + "/api/assignments/me/engagements/"
                                        + engagementId
                                        + "/exists"
                        )
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
                        .retrieve()
                        .body(Boolean.class);

        return Boolean.TRUE.equals(assigned);
    }

    /*
     * Best-effort cascade: an engagement's status/cancellation should still
     * succeed even if staffing can't be reached right now, so a failure
     * here (including the breaker being open) is logged and swallowed
     * rather than thrown back to the caller. The call still runs through
     * the breaker so repeated staffing outages fail fast instead of each
     * cascade blocking on a fresh connect/timeout.
     */
    public void cascadeAssignmentStatus(
        Long engagementId,
        String engagementStatus,
        String token){

        circuitBreakerFactory.create(CIRCUIT_BREAKER_ID)
                .run(
                        () -> {
                            doCascadeAssignmentStatus(engagementId, engagementStatus, token);
                            return null;
                        },
                        ex -> cascadeFallback(ex, "assignment status cascade", engagementId)
                );
    }

    private Void doCascadeAssignmentStatus(
            Long engagementId,
            String engagementStatus,
            String token) {

        ServiceInstance instance = getStaffingInstance();

        restClient.patch()
                .uri(
                        instance.getUri()
                                + "/api/assignments/engagement/{engagementId}/cascade-status",
                        engagementId
                )
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .body(
                        new CascadeAssignmentStatusRequest(
                                engagementStatus
                        )
                )
                .retrieve()
                .toBodilessEntity();

        return null;
    }

    public void cascadeEngagementCancelled(Long engagementId, String token) {

        circuitBreakerFactory.create(CIRCUIT_BREAKER_ID)
                .run(
                        () -> {
                            doCascadeEngagementCancelled(engagementId, token);
                            return null;
                        },
                        ex -> cascadeFallback(ex, "assignment cascade cancel", engagementId)
                );
    }

    private Void doCascadeEngagementCancelled(Long engagementId, String token) {

        ServiceInstance instance = getStaffingInstance();

        restClient.delete()
                .uri(
                        instance.getUri()
                                + "/api/assignments/engagement/{engagementId}",
                        engagementId
                )
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
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
    private <T> T fallback(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            throw rse;
        }

        log.warn("Staffing service call failed or circuit is open: {}", ex.getMessage());
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Unable to reach staffing service: " + ex.getMessage()
        );
    }

    private Void cascadeFallback(Throwable ex, String action, Long engagementId) {
        log.warn(
                "Failed to cascade {} for engagement id={} (staffing unavailable or circuit open): {}",
                action,
                engagementId,
                ex.getMessage()
        );
        return null;
    }

    private record CascadeAssignmentStatusRequest(
            String engagementStatus) {
    }
}
