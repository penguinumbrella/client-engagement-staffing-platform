package com.skillstorm.client.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillstorm.client.dtos.ClientRequest;
import com.skillstorm.client.dtos.ClientResponse;
import com.skillstorm.client.models.enums.RelationshipStatus;
import com.skillstorm.client.repositories.ClientRepository;
import com.skillstorm.client.services.ClientService;
import com.skillstorm.client.validation.UniqueCompanyNameValidator;

import java.util.List;

@WebMvcTest(ClientController.class)
@Import(UniqueCompanyNameValidator.class)
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClientService clientService;

    // Needed by UniqueCompanyNameValidator, which is pulled into the validation
    // pipeline whenever a request body is validated on the create endpoint.
    @MockitoBean
    private ClientRepository clientRepository;

    private ClientResponse clientResponse;
    private ClientRequest validRequest;

    @BeforeEach
    void setUp() {
        clientResponse = new ClientResponse(1L, "Acme Corp", "Manufacturing", "Jane Doe", "jane@acme.com", RelationshipStatus.ACTIVE);
        validRequest = new ClientRequest("Acme Corp", "Manufacturing", "Jane Doe", "jane@acme.com", RelationshipStatus.ACTIVE);
    }

    // ----- GET /clients -----

    @Test
    void getAllClients_returnsOk() throws Exception {
        Page<ClientResponse> page = new PageImpl<>(List.of(clientResponse));
        when(clientService.getAllClients(0)).thenReturn(ResponseEntity.ok(page));

        mockMvc.perform(get("/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].companyName").value("Acme Corp"));
    }

    // ----- GET /clients/{id} -----

    @Test
    void getClientById_found_returnsOk() throws Exception {
        when(clientService.getClientById(1L)).thenReturn(ResponseEntity.ok(clientResponse));

        mockMvc.perform(get("/clients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Acme Corp"));
    }

    @Test
    void getClientById_notFound_returnsNotFound() throws Exception {
        when(clientService.getClientById(99L)).thenReturn(ResponseEntity.notFound().build());

        mockMvc.perform(get("/clients/99"))
                .andExpect(status().isNotFound());
    }

    // ----- POST /clients -----

    @Test
    void createClient_valid_returnsCreated() throws Exception {
        when(clientService.createClient(any(ClientRequest.class))).thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(clientResponse));

        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName").value("Acme Corp"));
    }

    @Test
    void createClient_invalidBody_returnsBadRequest() throws Exception {
        ClientRequest invalidRequest = new ClientRequest("", null, null, null, null);

        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createClient_duplicateCompanyName_returnsBadRequest() throws Exception {
        when(clientRepository.existsByCompanyNameIgnoreCase(eq("Acme Corp"))).thenReturn(true);

        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createClient_conflictFromService_returnsConflict() throws Exception {
        when(clientService.createClient(any(ClientRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "A client named 'Acme Corp' already exists."));

        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict());
    }

    // ----- PUT /clients/{id} -----

    @Test
    void updateClient_success_returnsOk() throws Exception {
        when(clientService.updateClient(eq(1L), any(ClientRequest.class))).thenReturn(ResponseEntity.ok(clientResponse));

        mockMvc.perform(put("/clients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Acme Corp"));
    }

    @Test
    void updateClient_notFound_returnsNotFound() throws Exception {
        when(clientService.updateClient(eq(99L), any(ClientRequest.class))).thenReturn(ResponseEntity.notFound().build());

        mockMvc.perform(put("/clients/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateClient_conflictFromService_returnsConflict() throws Exception {
        when(clientService.updateClient(eq(1L), any(ClientRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "A client named 'Acme Corp' already exists."));

        mockMvc.perform(put("/clients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict());
    }

    // ----- DELETE /clients/{id} -----

    @Test
    void deleteClient_success_returnsNoContent() throws Exception {
        when(clientService.deleteClient(1L)).thenReturn(ResponseEntity.noContent().build());

        mockMvc.perform(delete("/clients/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteClient_notFound_returnsNotFound() throws Exception {
        when(clientService.deleteClient(99L)).thenReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).build());

        mockMvc.perform(delete("/clients/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteClient_hasActiveEngagements_returnsConflict() throws Exception {
        when(clientService.deleteClient(1L)).thenReturn(ResponseEntity.status(HttpStatus.CONFLICT).build());

        mockMvc.perform(delete("/clients/1"))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteClient_engagementServiceUnavailable_returnsServiceUnavailable() throws Exception {
        when(clientService.deleteClient(1L)).thenReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());

        mockMvc.perform(delete("/clients/1"))
                .andExpect(status().isServiceUnavailable());
    }
}
