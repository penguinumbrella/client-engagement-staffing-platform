package com.skillstorm.staffing.dto;

import com.skillstorm.staffing.enums.SkillArea;

public class UpdateConsultantRequest {

    private String name;
    private String titleRole;
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
