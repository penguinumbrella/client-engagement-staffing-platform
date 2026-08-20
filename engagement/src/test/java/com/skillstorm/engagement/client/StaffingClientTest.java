package com.skillstorm.engagement.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffingClientTest {

    @Mock
    private LoadBalancerClient loadBalancerClient;

    private StaffingClient staffingClient;

    @BeforeEach
    void setUp() {
        staffingClient = new StaffingClient(RestClient.builder(), loadBalancerClient);
    }

    @Test
    void cascadeAssignmentStatus_skipsSilentlyWhenStaffingInstanceUnavailable() {
        when(loadBalancerClient.choose(eq("staffing"))).thenReturn(null);

        assertThatCode(() -> staffingClient.cascadeAssignmentStatus(1L, "In Progress", "test-token")).doesNotThrowAnyException();

        verify(loadBalancerClient).choose("staffing");
        verifyNoMoreInteractions(loadBalancerClient);
    }

    @Test
    void cascadeAssignmentStatus_swallowsRestClientExceptionWhenCallFails() {
        ServiceInstance instance = new DefaultServiceInstance("staffing-1", "staffing", "localhost", 1, false);
        when(loadBalancerClient.choose(eq("staffing"))).thenReturn(instance);

        assertThatCode(() -> staffingClient.cascadeAssignmentStatus(1L, "In Progress", "test-token")).doesNotThrowAnyException();
    }

    @Test
    void cascadeEngagementCancelled_skipsSilentlyWhenStaffingInstanceUnavailable() {
        when(loadBalancerClient.choose(eq("staffing"))).thenReturn(null);

        assertThatCode(() -> staffingClient.cascadeEngagementCancelled(1L, "test-token")).doesNotThrowAnyException();

        verify(loadBalancerClient).choose("staffing");
        verifyNoMoreInteractions(loadBalancerClient);
    }

    @Test
    void cascadeEngagementCancelled_swallowsRestClientExceptionWhenCallFails() {
        ServiceInstance instance = new DefaultServiceInstance("staffing-1", "staffing", "localhost", 1, false);
        when(loadBalancerClient.choose(eq("staffing"))).thenReturn(instance);

        assertThatCode(() -> staffingClient.cascadeEngagementCancelled(1L, "test-token")).doesNotThrowAnyException();
    }
}
