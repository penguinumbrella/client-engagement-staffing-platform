package com.skillstorm.auth_service.Dtos;

import jakarta.validation.constraints.NotBlank;

public record CompleteProfileRequest(
        @NotBlank String titleRole,
        @NotBlank String primarySkillArea
) {}
