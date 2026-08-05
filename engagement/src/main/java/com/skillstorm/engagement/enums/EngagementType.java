package com.skillstorm.engagement.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EngagementType {

    AUDIT("Audit"),
    TAX_ADVISORY("Tax Advisory"),
    RISK_CONSULTING("Risk Consulting"),
    FINANCIAL_ADVISORY("Financial Advisory");

    private final String label;

    EngagementType(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static EngagementType fromValue(String value) {
        for (EngagementType type : values()) {
            if (type.label.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown engagement type: " + value);
    }
}
