package com.skillstorm.staffing.dto;

import com.skillstorm.staffing.model.Assignment;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public class AssignmentResponse {

    private final Long id;
    private final Long consultantId;
    private final String consultantName;
    private final Long engagementId;
    private final String engagementRole;
    private final LocalDate assignmentStartDate;
    private final boolean active;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public AssignmentResponse(Long id, Long consultantId, String consultantName, Long engagementId,
                               String engagementRole, LocalDate assignmentStartDate, boolean active,
                               OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.consultantId = consultantId;
        this.consultantName = consultantName;
        this.engagementId = engagementId;
        this.engagementRole = engagementRole;
        this.assignmentStartDate = assignmentStartDate;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AssignmentResponse from(Assignment assignment, String consultantName) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getConsultantId(),
                consultantName,
                assignment.getEngagementId(),
                assignment.getEngagementRole(),
                assignment.getAssignmentStartDate(),
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
