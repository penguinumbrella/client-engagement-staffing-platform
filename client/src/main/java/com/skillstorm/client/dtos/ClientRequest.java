package com.skillstorm.client.dtos;

import com.skillstorm.client.models.enums.RelationshipStatus;
import com.skillstorm.client.validation.UniqueCompanyName;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClientRequest(
    @NotBlank(message = "Company name is required")
    @Size(max = 100, message = "Company name must be less than 100 characters")
    @UniqueCompanyName
    String companyName,

    @Size(max = 50, message = "Industry must be less than 50 characters")
    String industry,

    @Size(max = 100, message = "Primary contact name must be less than 100 characters")
    String primaryContactName,

    @Size(max = 100, message = "Primary contact email must be less than 100 characters")
    String primaryContactEmail,

    @NotNull(message = "Relationship status is required")
    RelationshipStatus relationshipStatus
) {}