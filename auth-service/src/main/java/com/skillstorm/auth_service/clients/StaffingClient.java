package com.skillstorm.auth_service.clients;

import com.skillstorm.auth_service.Dtos.ProvisionConsultantRequest;

import java.util.UUID;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class StaffingClient {

    private final RestClient restClient;
    private final LoadBalancerClient loadBalancerClient;

    public StaffingClient(
            RestClient.Builder builder,
            LoadBalancerClient loadBalancerClient) {

        this.restClient = builder.build();
        this.loadBalancerClient = loadBalancerClient;
    }

    public void provisionConsultant(ProvisionConsultantRequest request, String token) {

        ServiceInstance instance =
                loadBalancerClient.choose("staffing");

        if (instance == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Staffing service is not available"
            );
        }

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
    }

    public void provisionConsultantByManager(
        UUID userId,
        ProvisionConsultantRequest request,
        String managerToken) {

        ServiceInstance instance =
                loadBalancerClient.choose("staffing");

        if (instance == null) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Staffing service is not available"
                );
        }

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
    }
}