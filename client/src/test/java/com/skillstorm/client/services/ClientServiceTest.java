package com.skillstorm.client.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import com.skillstorm.client.clients.EngagementClient;
import com.skillstorm.client.dtos.ClientRequest;
import com.skillstorm.client.dtos.ClientResponse;
import com.skillstorm.client.mappers.ClientMapper;
import com.skillstorm.client.models.Client;
import com.skillstorm.client.models.enums.RelationshipStatus;
import com.skillstorm.client.repositories.ClientRepository;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepo;

    @Mock
    private ClientMapper clientMapper;

    @Mock
    private EngagementClient engagementClient;

    @InjectMocks
    private ClientService clientService;

    private Client activeClient;
    private ClientResponse clientResponse;
    private ClientRequest clientRequest;

    @BeforeEach
    void setUp() {
        activeClient = new Client("Acme Corp", "Manufacturing", "Jane Doe", "jane@acme.com", RelationshipStatus.ACTIVE);
        activeClient.setId(1L);

        clientResponse = new ClientResponse(1L, "Acme Corp", "Manufacturing", "Jane Doe", "jane@acme.com", RelationshipStatus.ACTIVE);

        clientRequest = new ClientRequest("Acme Corp", "Manufacturing", "Jane Doe", "jane@acme.com", RelationshipStatus.ACTIVE);
    }

    // ----- getAllClients -----

    @Test
    void getAllClients_returnsOkWithMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Client> clientPage = new PageImpl<>(List.of(activeClient), pageable, 1);

        when(clientRepo.findByIsActiveTrue(any(Pageable.class))).thenReturn(clientPage);
        when(clientMapper.toDto(activeClient)).thenReturn(clientResponse);

        ResponseEntity<Page<ClientResponse>> result = clientService.getAllClients(0, 10);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().getTotalElements());
        assertEquals(clientResponse, result.getBody().getContent().get(0));
    }

    // ----- getClientById -----

    @Test
    void getClientById_activeClientFound_returnsOk() {
        when(clientRepo.findById(1L)).thenReturn(Optional.of(activeClient));
        when(clientMapper.toDto(activeClient)).thenReturn(clientResponse);

        ResponseEntity<ClientResponse> result = clientService.getClientById(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(clientResponse, result.getBody());
    }

    @Test
    void getClientById_inactiveClient_returnsNotFound() {
        activeClient.setActive(false);
        when(clientRepo.findById(1L)).thenReturn(Optional.of(activeClient));

        ResponseEntity<ClientResponse> result = clientService.getClientById(1L);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertNull(result.getBody());
    }

    @Test
    void getClientById_notFound_returnsNotFound() {
        when(clientRepo.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<ClientResponse> result = clientService.getClientById(99L);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertNull(result.getBody());
    }

    // ----- createClient -----

    @Test
    void createClient_success_returnsCreated() {
        when(clientRepo.save(any(Client.class))).thenReturn(activeClient);
        when(clientMapper.toDto(activeClient)).thenReturn(clientResponse);

        ResponseEntity<ClientResponse> result = clientService.createClient(clientRequest);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(clientResponse, result.getBody());
    }

    @Test
    void createClient_duplicateCompanyName_throwsConflict() {
        when(clientRepo.save(any(Client.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> clientService.createClient(clientRequest));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ----- updateClient -----

    @Test
    void updateClient_activeClientFound_returnsOk() {
        when(clientRepo.findById(1L)).thenReturn(Optional.of(activeClient));
        when(clientRepo.save(any(Client.class))).thenReturn(activeClient);
        when(clientMapper.toDto(activeClient)).thenReturn(clientResponse);

        ResponseEntity<ClientResponse> result = clientService.updateClient(1L, clientRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(clientResponse, result.getBody());
    }

    @Test
    void updateClient_duplicateCompanyName_throwsConflict() {
        when(clientRepo.findById(1L)).thenReturn(Optional.of(activeClient));
        when(clientRepo.save(any(Client.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> clientService.updateClient(1L, clientRequest));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void updateClient_inactiveClient_returnsNotFound() {
        activeClient.setActive(false);
        when(clientRepo.findById(1L)).thenReturn(Optional.of(activeClient));

        ResponseEntity<ClientResponse> result = clientService.updateClient(1L, clientRequest);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        verify(clientRepo, never()).save(any(Client.class));
    }

    @Test
    void updateClient_notFound_returnsNotFound() {
        when(clientRepo.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<ClientResponse> result = clientService.updateClient(99L, clientRequest);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    // ----- deleteClient -----

    @Test
    void deleteClient_activeClientNoEngagements_returnsNoContent() {
        when(clientRepo.findById(1L)).thenReturn(Optional.of(activeClient));
        when(engagementClient.hasActiveEngagements(1L, "test-token")).thenReturn(false);

        ResponseEntity<Void> result = clientService.deleteClient(1L, "test-token");

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(clientRepo, times(1)).save(activeClient);
    }

    @Test
    void deleteClient_hasActiveEngagements_returnsConflict() {
        when(clientRepo.findById(1L)).thenReturn(Optional.of(activeClient));
        when(engagementClient.hasActiveEngagements(1L, "test-token")).thenReturn(true);

        ResponseEntity<Void> result = clientService.deleteClient(1L, "test-token");

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
        verify(clientRepo, never()).save(any(Client.class));
    }

    @Test
    void deleteClient_engagementServiceUnavailable_returnsSameStatus() {
        when(clientRepo.findById(1L)).thenReturn(Optional.of(activeClient));
        when(engagementClient.hasActiveEngagements(1L, "test-token"))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Engagement service is not available"));

        ResponseEntity<Void> result = clientService.deleteClient(1L, "test-token");

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, result.getStatusCode());
        verify(clientRepo, never()).save(any(Client.class));
    }

    @Test
    void deleteClient_inactiveClient_returnsNotFound() {
        activeClient.setActive(false);
        when(clientRepo.findById(1L)).thenReturn(Optional.of(activeClient));

        ResponseEntity<Void> result = clientService.deleteClient(1L, "test-token");

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        verify(engagementClient, never()).hasActiveEngagements(anyLong(), anyString());
    }

    @Test
    void deleteClient_notFound_returnsNotFound() {
        when(clientRepo.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<Void> result = clientService.deleteClient(99L, "test-token");

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }
}
