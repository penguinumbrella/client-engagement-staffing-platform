package com.skillstorm.engagement.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Notifies the staffing service after an engagement's status transition commits, so it can
 * cascade a default status to assignments that haven't been manually overridden. Best-effort:
 * failures are logged, not propagated, since this is a side effect of the engagement update, not
 * a precondition for it.
 */
@Component
public class StaffingClient {

    private static final Logger log = LoggerFactory.getLogger(StaffingClient.class);

    private final RestClient restClient;
    private final LoadBalancerClient loadBalancerClient;

    public StaffingClient(RestClient.Builder restClientBuilder, LoadBalancerClient loadBalancerClient) {
        this.restClient = restClientBuilder.build();
        this.loadBalancerClient = loadBalancerClient;
    }

    public void cascadeAssignmentStatus(Long engagementId, String engagementStatus) {
        ServiceInstance instance = loadBalancerClient.choose("staffing");
        if (instance == null) {
            log.warn("Staffing service is not available; skipped assignment status cascade for engagement id={}",
                    engagementId);
            return;
        }

        try {
            restClient.patch()
                    .uri(instance.getUri() + "/api/assignments/engagement/{engagementId}/cascade-status", engagementId)
                    .body(new CascadeAssignmentStatusRequest(engagementStatus))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("Failed to cascade assignment status for engagement id={}: {}", engagementId, ex.getMessage());
        }
    }

    /**
     * Notifies staffing that this engagement was cancelled, so it can cancel and deactivate any
     * remaining active/pending assignments tied to it. Best-effort, same rationale as
     * {@link #cascadeAssignmentStatus}.
     */
    public void cascadeEngagementCancelled(Long engagementId) {
        ServiceInstance instance = loadBalancerClient.choose("staffing");
        if (instance == null) {
            log.warn("Staffing service is not available; skipped assignment cascade cancel for engagement id={}",
                    engagementId);
            return;
        }

        try {
            restClient.delete()
                    .uri(instance.getUri() + "/api/assignments/engagement/{engagementId}", engagementId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("Failed to cascade cancel assignments for engagement id={}: {}", engagementId, ex.getMessage());
        }
    }

    private record CascadeAssignmentStatusRequest(String engagementStatus) {
    }
}
