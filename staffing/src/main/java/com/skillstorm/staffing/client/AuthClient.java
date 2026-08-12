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

    public AuthUserResponse getUserByEmail(
        String email,
        String token) {

    return restClient
            .get()
            .uri(
                    "http://localhost:8084/api/users/by-email?email={email}",
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