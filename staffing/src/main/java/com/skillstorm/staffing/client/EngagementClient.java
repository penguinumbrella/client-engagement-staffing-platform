package com.skillstorm.staffing.client;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class EngagementClient {

    private final RestClient restClient;
    private final LoadBalancerClient loadBalancerClient;

    public EngagementClient(RestClient.Builder restClientBuilder, LoadBalancerClient loadBalancerClient) {
        this.restClient = restClientBuilder.build();
        this.loadBalancerClient = loadBalancerClient;
    }

    public boolean engagementExists(
        Long engagementId,
        String token) {

        ServiceInstance instance =
                loadBalancerClient.choose("engagement");

        if (instance == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Engagement service is not available"
            );
        }

        try {

            restClient.get()
                    .uri(
                            instance.getUri()
                                    + "/api/engagements/{id}",
                            engagementId
                    )
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + token
                    )
                    .retrieve()
                    .toBodilessEntity();

            return true;

        } catch (HttpClientErrorException.NotFound ex) {

            return false;

        } catch (RestClientException ex) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to reach engagement service: "
                            + ex.getMessage()
            );
        }
    }
}
