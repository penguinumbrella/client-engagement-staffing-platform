package com.skillstorm.client.mappers;

import org.springframework.stereotype.Component;

import com.skillstorm.client.dtos.ClientResponse;
import com.skillstorm.client.models.clients;

@Component
public class ClientMapper {

    public ClientResponse toDto(clients client) {
        return new ClientResponse(
            client.getId(),
            client.getCompanyName(),
            client.getIndustry(),
            client.getPrimaryContactName(),
            client.getPrimaryContactEmail(),
            client.getRelationshipStatus()
        );
    }
}