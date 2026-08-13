package com.skillstorm.staffing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consultant_id", nullable = false)
    private Long consultantId;

    @Column(name = "engagement_id", nullable = false)
    private Long engagementId;

    @Column(name = "engagement_role", nullable = false)
    private String engagementRole;

    @Column(name = "assignment_start_date", nullable = false)
    private LocalDate assignmentStartDate;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected Assignment() {
    }

    public Assignment(Long consultantId, Long engagementId, String engagementRole, LocalDate assignmentStartDate) {
        this.consultantId = consultantId;
        this.engagementId = engagementId;
        this.engagementRole = engagementRole;
        this.assignmentStartDate = assignmentStartDate;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getConsultantId() {
        return consultantId;
    }

    public Long getEngagementId() {
        return engagementId;
    }

    public String getEngagementRole() {
        return engagementRole;
    }

    public void setEngagementRole(String engagementRole) {
        this.engagementRole = engagementRole;
    }

    public LocalDate getAssignmentStartDate() {
        return assignmentStartDate;
    }

    public void setAssignmentStartDate(LocalDate assignmentStartDate) {
        this.assignmentStartDate = assignmentStartDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
