package com.skillstorm.staffing.controller;

import com.skillstorm.staffing.dto.ConsultantResponse;
import com.skillstorm.staffing.dto.CreateConsultantRequest;
import com.skillstorm.staffing.dto.UpdateConsultantRequest;
import com.skillstorm.staffing.service.ConsultantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/consultants")
public class ConsultantController {

    private final ConsultantService consultantService;

    public ConsultantController(
            ConsultantService consultantService) {

        this.consultantService = consultantService;
    }

    @PreAuthorize("hasRole('CONSULTANT')")
    @GetMapping("/me")
    public ResponseEntity<ConsultantResponse> getMe(
            @AuthenticationPrincipal Jwt jwt) {

        UUID authUserId =
                UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(
                consultantService.getByAuthUserId(authUserId)
        );
    }

    @PreAuthorize("hasRole('ENGAGEMENT_MANAGER')")
    @PostMapping
    public ResponseEntity<ConsultantResponse> create(@Valid @RequestBody CreateConsultantRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        consultantService.createConsultant(
                                request,
                                jwt.getTokenValue()
                        )
                );
    }

    @GetMapping
    public ResponseEntity<List<ConsultantResponse>> getAll() {

        return ResponseEntity.ok(
                consultantService.getAllConsultants()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConsultantResponse> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                consultantService.getConsultantById(id)
        );
    }

    @PreAuthorize("hasRole('ENGAGEMENT_MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<ConsultantResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateConsultantRequest request) {

        return ResponseEntity.ok(
                consultantService.updateConsultant(id, request)
        );
    }

    @PreAuthorize("hasRole('ENGAGEMENT_MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        consultantService.deleteConsultant(id);

        return ResponseEntity.noContent().build();
    }
}