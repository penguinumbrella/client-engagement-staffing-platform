package com.skillstorm.client.mappers;

import org.springframework.stereotype.Component;

import com.skillstorm.client.dtos.ClientResponse;
import com.skillstorm.client.models.Client;

@Component
public class ClientMapper {

    public ClientResponse toDto(Client client) {
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