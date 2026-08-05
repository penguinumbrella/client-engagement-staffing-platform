package com.skillstorm.client.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.skillstorm.client.dtos.ClientResponse;
import com.skillstorm.client.mappers.ClientMapper;
import com.skillstorm.client.models.clients;
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
        
        return ResponseEntity.ok(this.clientRepo.findAll(pages).map(clientMapper::toDto));
    }

    public ResponseEntity<ClientResponse> getClientById(Long id) {
        Optional<clients> clientOptional = clientRepo.findById(id);
    }

    public clients createClient(clients client) {
        return clientRepo.save(client);
    }

    public clients updateClient(Long id, clients client) {
        if (clientRepo.existsById(id)) {
            client.setId(id);
            return clientRepo.save(client);
        }
        return null;
    }

    public void deleteClient(Long id) {
        clientRepo.deleteById(id);
    }

}
