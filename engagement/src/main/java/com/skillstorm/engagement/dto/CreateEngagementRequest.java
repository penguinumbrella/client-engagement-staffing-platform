package com.skillstorm.engagement.dto;

import com.skillstorm.engagement.enums.EngagementStatus;
import com.skillstorm.engagement.enums.EngagementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class CreateEngagementRequest {

    @NotBlank(message = "engagementName is required")
    private String engagementName;

    @NotNull(message = "clientId is required")
    private Long clientId;

    @NotNull(message = "engagementType is required")
    private EngagementType engagementType;

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    @NotNull(message = "targetEndDate is required")
    private LocalDate targetEndDate;

    private EngagementStatus status;

    public String getEngagementName() {
        return engagementName;
    }

    public void setEngagementName(String engagementName) {
        this.engagementName = engagementName;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public EngagementType getEngagementType() {
        return engagementType;
    }

    public void setEngagementType(EngagementType engagementType) {
        this.engagementType = engagementType;
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
