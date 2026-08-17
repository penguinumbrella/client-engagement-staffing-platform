package com.skillstorm.staffing.client;

import com.skillstorm.staffing.dto.AuthUserResponse;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthClient {

    private final RestClient restClient;
    private final LoadBalancerClient loadBalancerClient;

    public AuthClient(
            RestClient.Builder builder,
            LoadBalancerClient loadBalancerClient) {

        this.restClient = builder.build();
        this.loadBalancerClient = loadBalancerClient;
    }

    public AuthUserResponse getUserByEmail(String email, String token) {

        ServiceInstance instance =
                loadBalancerClient.choose("auth-service");

        if (instance == null) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Auth service is not available"
                );
        }

        return restClient
                .get()
                .uri(
                        instance.getUri()
                                + "/api/users/by-email?email={email}",
                        email
                )
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .retrieve()
                .body(AuthUserResponse.class);
    }
}