package com.skillstorm.client.clients;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class EngagementClient {

    private final RestClient restClient;
    private final LoadBalancerClient loadBalancerClient;

    public EngagementClient(RestClient.Builder restClientBuilder, LoadBalancerClient loadBalancerClient) {
        this.restClient = restClientBuilder.build();
        this.loadBalancerClient = loadBalancerClient;
    }

    public boolean hasActiveEngagements(Long clientId) {
        ServiceInstance instance = loadBalancerClient.choose("engagement");
        if (instance == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Engagement service is not available");
        }

        try {
            List<Object> engagements = restClient.get()
                    .uri(instance.getUri() + "/api/engagements/client/{clientId}", clientId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Object>>() {});
            return engagements != null && !engagements.isEmpty(); 
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to reach engagement service: " + ex.getMessage());
        }
    }
}
