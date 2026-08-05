package com.skillstorm.staffing.dto;

import com.skillstorm.staffing.enums.SkillArea;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateConsultantRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "titleRole is required")
    private String titleRole;

    @NotNull(message = "primarySkillArea is required")
    private SkillArea primarySkillArea;

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

    public SkillArea getPrimarySkillArea() {
        return primarySkillArea;
    }

    public void setPrimarySkillArea(SkillArea primarySkillArea) {
        this.primarySkillArea = primarySkillArea;
    }
}
