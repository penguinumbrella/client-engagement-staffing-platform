package com.skillstorm.engagement.client;

import com.skillstorm.engagement.dto.AuthUserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class AuthClient {

    private static final Logger log = LoggerFactory.getLogger(AuthClient.class);

    private final RestClient restClient;
    private final LoadBalancerClient loadBalancerClient;

    public AuthClient(
            RestClient.Builder builder,
            LoadBalancerClient loadBalancerClient) {

        this.restClient = builder.build();
        this.loadBalancerClient = loadBalancerClient;
    }

    /**
     * Used only to resolve who to broadcast an engagement-manager
     * notification to — best-effort, like StaffingClient's staffed-consultant
     * lookup. A create/update/delete/cancel that already persisted its
     * change shouldn't fail (and appear to the caller as if nothing
     * happened) just because auth is unreachable when it's time to notify
     * the other EMs; skip the broadcast instead.
     */
    public List<AuthUserResponse> getUsersByRole(String role, String token) {

        ServiceInstance instance =
                loadBalancerClient.choose("auth-service");

        if (instance == null) {
            log.warn(
                    "Auth service is not available; skipped engagement-manager broadcast lookup for role={}",
                    role
            );
            return List.of();
        }

        try {
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

        } catch (RestClientException ex) {
            log.warn(
                    "Failed to fetch users with role={} for engagement-manager broadcast: {}",
                    role,
                    ex.getMessage()
            );
            return List.of();
        }
    }
}
