package com.skillstorm.staffing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "consultants")
public class Consultant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "title_role", nullable = false)
    private String titleRole;

    @Column(name = "primary_skill_area", nullable = false)
    private String primarySkillArea;

    @Column(name = "user_id", unique = true, nullable = false)
    private UUID userId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected Consultant() {
    }

    public Consultant(String name, String titleRole, String primarySkillArea, UUID userId) {
        this.name = name;
        this.titleRole = titleRole;
        this.primarySkillArea = primarySkillArea;
        this.userId = userId;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitleRole() {
        return titleRole;
    }

    public void setTitleRole(String titleRole) {
        this.titleRole = titleRole;
    }

    public String getPrimarySkillArea() {
        return primarySkillArea;
    }

    public void setPrimarySkillArea(String primarySkillArea) {
        this.primarySkillArea = primarySkillArea;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
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
