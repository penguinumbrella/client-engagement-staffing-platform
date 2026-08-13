package com.skillstorm.client.services;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.skillstorm.client.dtos.ClientRequest;
import com.skillstorm.client.dtos.ClientResponse;
import com.skillstorm.client.mappers.ClientMapper;
import com.skillstorm.client.models.Client;
import com.skillstorm.client.repositories.ClientRepository;

@Service
public class ClientService {

    private final ClientRepository clientRepo;
    private final ClientMapper clientMapper;
    private static final int PAGE_SIZE = 10;
    
    public ClientService(ClientRepository clientRepo, ClientMapper clientMapper) {
        this.clientRepo = clientRepo;
        this.clientMapper = clientMapper;
    }

    public ResponseEntity<Page<ClientResponse>> getAllClients(int page) {
        Pageable pages = PageRequest.of(page, PAGE_SIZE);

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
        
        Client client = this.clientRepo.save(new Client(dto.companyName(), dto.industry(), dto.primaryContactName(), dto.primaryContactEmail(), dto.relationshipStatus()));

        return ResponseEntity.status(201).body(this.clientMapper.toDto(client));
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

            Client updated = this.clientRepo.save(temp);

            return ResponseEntity.ok().body(this.clientMapper.toDto(updated));
        }
        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<Void> deleteClient(Long id) {
        Optional<Client> current = this.clientRepo.findById(id);

        if(current.isPresent() && current.get().isActive()) {

        /**
         * Later 409 Logic for Client Engagements
         *
         *   try {
         *      logic for checking Engagement Service
         *      return ResponseEntity.status(409).build();
         *   } catch (fallback logic) {
         *      return ResponseEntity.status(503).build();
         *   }
         *
         */

            Client temp = current.get();
            temp.setActive(false);
            this.clientRepo.save(temp);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(404).build();
    }
}
