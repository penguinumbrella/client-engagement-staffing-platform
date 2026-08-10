package com.skillstorm.engagement.dto;

import com.skillstorm.engagement.enums.EngagementStatus;
import com.skillstorm.engagement.enums.EngagementType;

import java.time.LocalDate;

/**
 * Partial-update payload. Only name, status, timeline, type, and summary are editable per spec;
 * null fields are left unchanged.
 */
public class UpdateEngagementRequest {

    private String engagementName;
    private EngagementType engagementType;
    private String summary;
    private LocalDate startDate;
    private LocalDate targetEndDate;
    private EngagementStatus status;

    public String getEngagementName() {
        return engagementName;
    }

    public void setEngagementName(String engagementName) {
        this.engagementName = engagementName;
    }

    public EngagementType getEngagementType() {
        return engagementType;
    }

    public void setEngagementType(EngagementType engagementType) {
        this.engagementType = engagementType;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getTargetEndDate() {
        return targetEndDate;
    }

    public void setTargetEndDate(LocalDate targetEndDate) {
        this.targetEndDate = targetEndDate;
    }

    public EngagementStatus getStatus() {
        return status;
    }

    public void setStatus(EngagementStatus status) {
        this.status = status;
    }
}
