package com.skillstorm.client.clients;

import com.skillstorm.client.dtos.AuthUserResponse;
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
public class AuthClient {

    private final RestClient restClient;
    private final LoadBalancerClient loadBalancerClient;

    public AuthClient(
            RestClient.Builder builder,
            LoadBalancerClient loadBalancerClient) {

        this.restClient = builder.build();
        this.loadBalancerClient = loadBalancerClient;
    }

    public List<AuthUserResponse> getUsersByRole(String role, String token) {

        ServiceInstance instance =
                loadBalancerClient.choose("auth-service");

        if (instance == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Auth service is not available"
            );
        }

        List<AuthUserResponse> users =
                restClient
                        .get()
                        .uri(
                                instance.getUri()
                                        + "/api/users?role={role}",
                                role
                        )
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
                        .retrieve()
                        .body(
                                new ParameterizedTypeReference<List<AuthUserResponse>>() {
                                }
                        );

        return users != null ? users : List.of();
    }
}
