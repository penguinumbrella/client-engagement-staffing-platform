package com.skillstorm.staffing.dto;

import com.skillstorm.staffing.enums.SkillArea;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProvisionConsultantRequest(

        @NotBlank
        String firstName,

        @NotBlank
        String lastName,

        @NotBlank
        String titleRole,

        @NotNull
        SkillArea primarySkillArea

) {}