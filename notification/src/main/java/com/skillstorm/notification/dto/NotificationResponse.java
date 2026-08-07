package com.skillstorm.notification.dto;

import com.skillstorm.notification.model.Notification;

import java.time.OffsetDateTime;

public class NotificationResponse {

    private final Long id;
    private final Long recipientId;
    private final String title;
    private final String message;
    private final String type;
    private final String sourceService;
    private final Long sourceId;
    private final boolean read;
    private final boolean active;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public NotificationResponse(Long id, Long recipientId, String title, String message, String type,
                                String sourceService, Long sourceId, boolean read, boolean active,
                                OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.recipientId = recipientId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.sourceService = sourceService;
        this.sourceId = sourceId;
        this.read = read;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getRecipientId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.getSourceService(),
                notification.getSourceId(),
                notification.isRead(),
                notification.isActive(),
                notification.getCreatedAt(),
                notification.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public String getSourceService() {
        return sourceService;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public boolean isRead() {
        return read;
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
