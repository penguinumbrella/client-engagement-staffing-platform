package com.skillstorm.staffing.dto;

import com.skillstorm.staffing.enums.EngagementRole;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class CreateAssignmentRequest {

    @NotNull(message = "consultantId is required")
    private Long consultantId;

    @NotNull(message = "engagementId is required")
    private Long engagementId;

    @NotNull(message = "engagementRole is required")
    private EngagementRole engagementRole;

    @NotNull(message = "assignmentStartDate is required")
    private LocalDate assignmentStartDate;

    public Long getConsultantId() {
        return consultantId;
    }

    public void setConsultantId(Long consultantId) {
        this.consultantId = consultantId;
    }

    public Long getEngagementId() {
        return engagementId;
    }

    public void setEngagementId(Long engagementId) {
        this.engagementId = engagementId;
    }

    public EngagementRole getEngagementRole() {
        return engagementRole;
    }

    public void setEngagementRole(EngagementRole engagementRole) {
        this.engagementRole = engagementRole;
    }

    public LocalDate getAssignmentStartDate() {
        return assignmentStartDate;
    }

    public void setAssignmentStartDate(LocalDate assignmentStartDate) {
        this.assignmentStartDate = assignmentStartDate;
    }
}
