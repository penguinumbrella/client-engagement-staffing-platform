package com.skillstorm.engagement.client;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ClientClient {

    private final RestClient restClient;
    private final LoadBalancerClient loadBalancerClient;

    public ClientClient(
            RestClient.Builder builder,
            LoadBalancerClient loadBalancerClient) {

        this.restClient = builder.build();
        this.loadBalancerClient = loadBalancerClient;
    }

    public void validateClientExists(
            Long clientId,
            String token) {

        ServiceInstance instance =
                loadBalancerClient.choose("client");

        if (instance == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Client service is not available"
            );
        }

        try {

            restClient
                    .get()
                    .uri(
                            instance.getUri()
                                    + "/clients/{clientId}",
                            clientId
                    )
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + token
                    )
                    .retrieve()
                    .toBodilessEntity();

        } catch (HttpClientErrorException.NotFound ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Client " + clientId + " does not exist"
            );

        } catch (RestClientResponseException ex) {

            throw new ResponseStatusException(
                    ex.getStatusCode(),
                    "Client validation failed",
                    ex
            );

        } catch (RestClientException ex) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Client service is unavailable",
                    ex
            );
        }
    }
}