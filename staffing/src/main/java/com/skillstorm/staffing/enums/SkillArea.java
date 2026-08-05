package com.skillstorm.staffing.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SkillArea {

    AUDIT("Audit"),
    TAX("Tax"),
    RISK("Risk"),
    TECHNOLOGY("Technology"),
    STRATEGY("Strategy");

    private final String label;

    SkillArea(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static SkillArea fromValue(String value) {
        for (SkillArea area : values()) {
            if (area.label.equalsIgnoreCase(value) || area.name().equalsIgnoreCase(value)) {
                return area;
            }
        }
        throw new IllegalArgumentException("Unknown primary skill area: " + value);
    }
}
