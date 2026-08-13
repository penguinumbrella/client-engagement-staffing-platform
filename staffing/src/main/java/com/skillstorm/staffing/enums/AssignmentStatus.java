package com.skillstorm.staffing.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AssignmentStatus {

    ACTIVE("Active"),
    PENDING("Pending"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String label;

    AssignmentStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static AssignmentStatus fromValue(String value) {
        for (AssignmentStatus status : values()) {
            if (status.label.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown assignment status: " + value);
    }
}
