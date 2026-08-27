package com.skillstorm.engagement.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Component
public class StaffingClient {

    private static final Logger log =
            LoggerFactory.getLogger(StaffingClient.class);

    private final RestClient restClient;
    private final LoadBalancerClient loadBalancerClient;

    public StaffingClient(
            RestClient.Builder restClientBuilder,
            LoadBalancerClient loadBalancerClient) {

        this.restClient = restClientBuilder.build();
        this.loadBalancerClient = loadBalancerClient;
    }

    public List<Long> getCurrentUserEngagementIds(String token) {

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

    public void cascadeAssignmentStatus(
        Long engagementId,
        String engagementStatus,
        String token){

        ServiceInstance instance =
                loadBalancerClient.choose("staffing");

        if (instance == null) {
            log.warn(
                    "Staffing service is not available; skipped assignment status cascade for engagement id={}",
                    engagementId
            );
            return;
        }

        try {
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

        } catch (RestClientException ex) {
            log.warn(
                    "Failed to cascade assignment status for engagement id={}: {}",
                    engagementId,
                    ex.getMessage()
            );
        }
    }

    public void cascadeEngagementCancelled(Long engagementId, String token) {

        ServiceInstance instance =
                loadBalancerClient.choose("staffing");

        if (instance == null) {
            log.warn(
                    "Staffing service is not available; skipped assignment cascade cancel for engagement id={}",
                    engagementId
            );
            return;
        }

        try {
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

        } catch (RestClientException ex) {
            log.warn(
                    "Failed to cascade cancel assignments for engagement id={}: {}",
                    engagementId,
                    ex.getMessage()
            );
        }
    }

    /**
     * The user UUIDs of every consultant currently (actively) staffed on
     * an engagement, for fanning out engagement-change notifications to
     * them. Falls back to an empty list if staffing is unreachable —
     * a missed consultant notification isn't worth failing the whole
     * engagement update over.
     */
    public List<UUID> getStaffedConsultantUserIds(Long engagementId, String token) {

        ServiceInstance instance =
                loadBalancerClient.choose("staffing");

        if (instance == null) {
            log.warn(
                    "Staffing service is not available; skipped consultant notification lookup for engagement id={}",
                    engagementId
            );
            return List.of();
        }

        try {
            List<StaffedAssignmentSummary> assignments =
                    restClient
                            .get()
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
                            .body(
                                    new ParameterizedTypeReference<List<StaffedAssignmentSummary>>() {
                                    }
                            );

            if (assignments == null) {
                return List.of();
            }

            return assignments
                    .stream()
                    .map(StaffedAssignmentSummary::consultantUserId)
                    .filter(userId -> userId != null)
                    .distinct()
                    .toList();

        } catch (RestClientException ex) {
            log.warn(
                    "Failed to fetch staffed consultants for engagement id={}: {}",
                    engagementId,
                    ex.getMessage()
            );
            return List.of();
        }
    }

    /** Only the fields needed to notify staffed consultants; other AssignmentResponse fields are ignored. */
    private record StaffedAssignmentSummary(UUID consultantUserId) {
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

    private record CascadeAssignmentStatusRequest(
            String engagementStatus) {
    }
}