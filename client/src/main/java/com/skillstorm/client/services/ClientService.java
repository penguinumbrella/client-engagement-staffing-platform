package com.skillstorm.client.services;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.skillstorm.client.clients.EngagementClient;
import com.skillstorm.client.dtos.ClientRequest;
import com.skillstorm.client.dtos.ClientResponse;
import com.skillstorm.client.kafka.NotificationEvent;
import com.skillstorm.client.kafka.NotificationEventPublisher;
import com.skillstorm.client.mappers.ClientMapper;
import com.skillstorm.client.models.Client;
import com.skillstorm.client.repositories.ClientRepository;

@Service
public class ClientService {

    private final ClientRepository clientRepo;
    private final ClientMapper clientMapper;
    private final EngagementClient engagementClient;
    private final NotificationEventPublisher notificationEventPublisher;

    public ClientService(ClientRepository clientRepo, ClientMapper clientMapper, EngagementClient engagementClient,
            NotificationEventPublisher notificationEventPublisher) {
        this.clientRepo = clientRepo;
        this.clientMapper = clientMapper;
        this.engagementClient = engagementClient;
        this.notificationEventPublisher = notificationEventPublisher;
    }

    public ResponseEntity<Page<ClientResponse>> getAllClients(int page, int size) {
        Pageable pages = PageRequest.of(page, size);

        return ResponseEntity.ok(this.clientRepo.findByIsActiveTrue(pages).map(clientMapper::toDto));
    }

    public ResponseEntity<ClientResponse> getClientById(Long id) {
        Optional<Client> temp = clientRepo.findById(id);

        if(temp.isPresent() && temp.get().isActive()) {
            return ResponseEntity.ok(this.clientMapper.toDto(temp.get()));
        }
        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<ClientResponse> createClient(ClientRequest dto) {

        try {
            Client client = this.clientRepo.save(new Client(dto.companyName(), dto.industry(), dto.primaryContactName(), dto.primaryContactEmail(), dto.relationshipStatus()));

            notificationEventPublisher.publish(new NotificationEvent(
                    "CLIENT_CREATED",
                    "client",
                    client.getId(),
                    client.getId(),
                    "New client",
                    "Client \"" + client.getCompanyName() + "\" was created."
            ));

            return ResponseEntity.status(201).body(this.clientMapper.toDto(client));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A client named '" + dto.companyName() + "' already exists.");
        }
    }

    public ResponseEntity<ClientResponse> updateClient(Long id, ClientRequest dto) {
        Optional<Client> current = this.clientRepo.findById(id);
        if(current.isPresent() && current.get().isActive()) {
            Client temp = current.get();

            if(dto.companyName() != null) temp.setCompanyName(dto.companyName());
            if(dto.industry() != null) temp.setIndustry(dto.industry());
            if(dto.primaryContactName() != null) temp.setPrimaryContactName(dto.primaryContactName());
            if(dto.primaryContactEmail() != null) temp.setPrimaryContactEmail(dto.primaryContactEmail());
            if(dto.relationshipStatus() != null) temp.setRelationshipStatus(dto.relationshipStatus());

            try {
                Client updated = this.clientRepo.save(temp);
                return ResponseEntity.ok().body(this.clientMapper.toDto(updated));
            } catch (DataIntegrityViolationException ex) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A client named '" + dto.companyName() + "' already exists.");
            }
        }
        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<Void> deleteClient(Long id, String token) {
        Optional<Client> current = this.clientRepo.findById(id);

        if(current.isPresent() && current.get().isActive()) {

            try {
                if (engagementClient.hasActiveEngagements(id, token)) {
                return ResponseEntity.status(409).build();
            }
            } catch (ResponseStatusException ex) {
                return ResponseEntity.status(ex.getStatusCode()).build();
            }

            Client temp = current.get();
            temp.setActive(false);
            this.clientRepo.save(temp);

            notificationEventPublisher.publish(new NotificationEvent(
                    "CLIENT_DELETED",
                    "client",
                    temp.getId(),
                    temp.getId(),
                    "Client removed",
                    "Client \"" + temp.getCompanyName() + "\" was deactivated."
            ));

            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(404).build();
    }
}
