package com.skillstorm.client.controllers;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.client.dtos.ClientRequest;
import com.skillstorm.client.dtos.ClientResponse;
import com.skillstorm.client.services.ClientService;

import jakarta.validation.Valid;

import java.util.UUID;

@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public ResponseEntity<Page<ClientResponse>> getAllClients(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return clientService.getAllClients(page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClientById(@PathVariable Long id) {
        return clientService.getClientById(id);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ClientResponse>> searchClients(@RequestParam String q) {
        return clientService.searchClients(q);
    }

    @PreAuthorize("hasRole('ENGAGEMENT_MANAGER')")
    @PostMapping
    public ResponseEntity<ClientResponse> createClient(
            @Valid @RequestBody ClientRequest dto,
            @AuthenticationPrincipal Jwt jwt) {

        return clientService.createClient(
                dto,
                jwt.getTokenValue(),
                UUID.fromString(jwt.getSubject())
        );
    }

    @PreAuthorize("hasRole('ENGAGEMENT_MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> updateClient(
            @PathVariable Long id,
            @RequestBody ClientRequest dto) {

        return clientService.updateClient(id, dto);
    }

    @PreAuthorize("hasRole('ENGAGEMENT_MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return clientService.deleteClient(
                id,
                jwt.getTokenValue(),
                UUID.fromString(jwt.getSubject())
        );
    }

}
