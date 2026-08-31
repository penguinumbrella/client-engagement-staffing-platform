package com.skillstorm.engagement.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ClientClient.class);
    private static final String CIRCUIT_BREAKER_ID = "client";

    private final RestClient restClient;
    private final LoadBalancerClient loadBalancerClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public ClientClient(
            RestClient.Builder builder,
            LoadBalancerClient loadBalancerClient,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {

        this.restClient = builder.build();
        this.loadBalancerClient = loadBalancerClient;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public void validateClientExists(
            Long clientId,
            String token) {

        circuitBreakerFactory.create(CIRCUIT_BREAKER_ID)
                .run(() -> doValidateClientExists(clientId, token), this::fallback);
    }

    private Void doValidateClientExists(
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

        return null;
    }

    /*
     * Runs when the underlying call fails (including a call timeout)
     * or the breaker is open and short-circuiting calls. Business errors
     * (bad request, validation failure) are already translated into a
     * ResponseStatusException by doValidateClientExists above, so they
     * just pass through here unchanged.
     */
    private Void fallback(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            throw rse;
        }

        log.warn("Client service call failed or circuit is open: {}", ex.getMessage());
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Unable to reach client service: " + ex.getMessage()
        );
    }
}
