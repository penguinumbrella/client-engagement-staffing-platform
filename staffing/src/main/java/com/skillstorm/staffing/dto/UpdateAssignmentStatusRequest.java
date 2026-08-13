package com.skillstorm.staffing.dto;

import com.skillstorm.staffing.enums.AssignmentStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateAssignmentStatusRequest {

    @NotNull(message = "status is required")
    private AssignmentStatus status;

    public AssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssignmentStatus status) {
        this.status = status;
    }
}
