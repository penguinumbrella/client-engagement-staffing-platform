package com.skillstorm.engagement.client;

import org.springframework.cloud.client.ServiceInstance;
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

    private final RestClient restClient;
    private final LoadBalancerClient loadBalancerClient;

    public StaffingClient(
            RestClient.Builder restClientBuilder,
            LoadBalancerClient loadBalancerClient) {

        this.restClient = restClientBuilder.build();
        this.loadBalancerClient = loadBalancerClient;
    }

    public List<Long> getCurrentUserEngagementIds(
            String token) {

        ServiceInstance instance =
                getStaffingInstance();

        List<Long> engagementIds =
                restClient
                        .get()
                        .uri(
                                instance.getUri()
                                        + "/api/assignments/me/engagement-ids"
                        )
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

        ServiceInstance instance =
                getStaffingInstance();

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
}