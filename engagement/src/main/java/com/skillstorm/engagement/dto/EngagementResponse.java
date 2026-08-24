package com.skillstorm.engagement.dto;

import com.skillstorm.engagement.model.Engagement;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public class EngagementResponse {

    private final Long id;
    private final String engagementName;
    private final Long clientId;
    private final String engagementType;
    private final String summary;
    private final LocalDate startDate;
    private final LocalDate targetEndDate;
    private final String status;
    private final boolean active;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public EngagementResponse(Long id, String engagementName, Long clientId, String engagementType, String summary,
                               LocalDate startDate, LocalDate targetEndDate, String status, boolean active,
                               OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.engagementName = engagementName;
        this.clientId = clientId;
        this.engagementType = engagementType;
        this.summary = summary;
        this.startDate = startDate;
        this.targetEndDate = targetEndDate;
        this.status = status;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static EngagementResponse from(Engagement engagement) {
        return new EngagementResponse(
                engagement.getId(),
                engagement.getEngagementName(),
                engagement.getClientId(),
                engagement.getEngagementType(),
                engagement.getSummary(),
                engagement.getStartDate(),
                engagement.getTargetEndDate(),
                engagement.getStatus(),
                engagement.isActive(),
                engagement.getCreatedAt(),
                engagement.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public String getEngagementName() {
        return engagementName;
    }

    public Long getClientId() {
        return clientId;
    }

    public String getEngagementType() {
        return engagementType;
    }

    public String getSummary() {
        return summary;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getTargetEndDate() {
        return targetEndDate;
    }

    public String getStatus() {
        return status;
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
