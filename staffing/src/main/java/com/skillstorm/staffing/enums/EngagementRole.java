package com.skillstorm.staffing.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EngagementRole {

    LEAD("Lead"),
    SENIOR_ASSOCIATE("Senior Associate"),
    ASSOCIATE("Associate");

    private final String label;

    EngagementRole(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static EngagementRole fromValue(String value) {
        for (EngagementRole role : values()) {
            if (role.label.equalsIgnoreCase(value) || role.name().equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown engagement role: " + value);
    }
}
