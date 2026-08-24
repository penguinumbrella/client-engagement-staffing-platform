package com.skillstorm.client.dtos;

import com.skillstorm.client.models.enums.RelationshipStatus;

public record ClientResponse(
    Long id,
    String companyName,
    String industry,
    String primaryContactName,
    String primaryContactEmail,
    RelationshipStatus relationshipStatus
) {}