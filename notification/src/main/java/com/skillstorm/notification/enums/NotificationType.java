package com.skillstorm.notification.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NotificationType {
    ENGAGEMENT_CREATED("ENGAGEMENT_CREATED"),
    ENGAGEMENT_UPDATED("ENGAGEMENT_UPDATED"),
    ENGAGEMENT_CANCELLED("ENGAGEMENT_CANCELLED"),
    ENGAGEMENT_DELETED("ENGAGEMENT_DELETED"),
    ASSIGNMENT_CREATED("ASSIGNMENT_CREATED"),
    CLIENT_CREATED("CLIENT_CREATED"),
    CLIENT_DELETED("CLIENT_DELETED"),
    ASSIGNMENT_UPDATED("ASSIGNMENT_UPDATED"),
    ASSIGNMENT_REMOVED("ASSIGNMENT_REMOVED"),
    ASSIGNMENT_CANCELLED("ASSIGNMENT_CANCELLED"),
    GENERAL("GENERAL");

    private final String label;

    NotificationType(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static NotificationType fromLabel(String label) {
        if (label == null || label.isBlank()) {
            return GENERAL;
        }
        for (NotificationType type : values()) {
            if (type.label.equalsIgnoreCase(label.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown notification type: " + label);
    }
}
