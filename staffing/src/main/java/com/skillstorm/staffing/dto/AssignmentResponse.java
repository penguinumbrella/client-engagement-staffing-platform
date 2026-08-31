package com.skillstorm.staffing.dto;

import com.skillstorm.staffing.model.Assignment;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public class AssignmentResponse {

    private final Long id;
    private final Long consultantId;
    private final UUID consultantUserId;
    private final String consultantName;
    private final Long engagementId;
    private final String engagementRole;
    private final LocalDate assignmentStartDate;
    private final LocalDate assignmentEndDate;
    private final String status;
    private final boolean statusOverridden;
    private final boolean active;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public AssignmentResponse(Long id, Long consultantId, UUID consultantUserId, String consultantName, Long engagementId,
                               String engagementRole, LocalDate assignmentStartDate, LocalDate assignmentEndDate,
                               String status, boolean statusOverridden, boolean active,
                               OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.consultantId = consultantId;
        this.consultantUserId = consultantUserId;
        this.consultantName = consultantName;
        this.engagementId = engagementId;
        this.engagementRole = engagementRole;
        this.assignmentStartDate = assignmentStartDate;
        this.assignmentEndDate = assignmentEndDate;
        this.status = status;
        this.statusOverridden = statusOverridden;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AssignmentResponse from(Assignment assignment, String consultantName, UUID consultantUserId) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getConsultantId(),
                consultantUserId,
                consultantName,
                assignment.getEngagementId(),
                assignment.getEngagementRole(),
                assignment.getAssignmentStartDate(),
                assignment.getAssignmentEndDate(),
                assignment.getStatus(),
                assignment.isStatusOverridden(),
                assignment.isActive(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getConsultantId() {
        return consultantId;
    }

    public UUID getConsultantUserId() {
        return consultantUserId;
    }

    public String getConsultantName() {
        return consultantName;
    }

    public Long getEngagementId() {
        return engagementId;
    }

    public String getEngagementRole() {
        return engagementRole;
    }

    public LocalDate getAssignmentStartDate() {
        return assignmentStartDate;
    }

    public LocalDate getAssignmentEndDate() {
        return assignmentEndDate;
    }

    public String getStatus() {
        return status;
    }

    public boolean isStatusOverridden() {
        return statusOverridden;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
