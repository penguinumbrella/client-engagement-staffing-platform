package com.skillstorm.auth_service.Dtos;

public record ProvisionConsultantRequest(
        String firstName,
        String lastName,
        String titleRole,
        String primarySkillArea
) {}
