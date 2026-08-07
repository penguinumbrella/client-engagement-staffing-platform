package com.skillstorm.engagement.kafka;

public class NotificationEvent {

    private String eventType;
    private String sourceService;
    private Long sourceId;
    private Long recipientId;
    private String title;
    private String message;

    public NotificationEvent() {
    }

    public NotificationEvent(String eventType, String sourceService, Long sourceId,
                             Long recipientId, String title, String message) {
        this.eventType = eventType;
        this.sourceService = sourceService;
        this.sourceId = sourceId;
        this.recipientId = recipientId;
        this.title = title;
        this.message = message;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSourceService() {
        return sourceService;
    }

    public void setSourceService(String sourceService) {
        this.sourceService = sourceService;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
