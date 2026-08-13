package com.skillstorm.staffing.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Sent by the engagement service after it commits its own status transition, so staffing can
 * cascade a default assignment status to any assignment that hasn't been manually overridden.
 */
public class CascadeAssignmentStatusRequest {

    @NotBlank(message = "engagementStatus is required")
    private String engagementStatus;

    public String getEngagementStatus() {
        return engagementStatus;
    }

    public void setEngagementStatus(String engagementStatus) {
        this.engagementStatus = engagementStatus;
    }
}
