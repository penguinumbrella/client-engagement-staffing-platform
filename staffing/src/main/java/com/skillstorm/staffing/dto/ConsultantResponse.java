package com.skillstorm.staffing.dto;

import com.skillstorm.staffing.model.Consultant;

import java.time.OffsetDateTime;

public class ConsultantResponse {

    private final Long id;
    private final String name;
    private final String titleRole;
    private final String primarySkillArea;
    private final boolean active;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public ConsultantResponse(Long id, String name, String titleRole, String primarySkillArea,
                               boolean active, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.titleRole = titleRole;
        this.primarySkillArea = primarySkillArea;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ConsultantResponse from(Consultant consultant) {
        return new ConsultantResponse(
                consultant.getId(),
                consultant.getName(),
                consultant.getTitleRole(),
                consultant.getPrimarySkillArea(),
                consultant.isActive(),
                consultant.getCreatedAt(),
                consultant.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTitleRole() {
        return titleRole;
    }

    public String getPrimarySkillArea() {
        return primarySkillArea;
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
