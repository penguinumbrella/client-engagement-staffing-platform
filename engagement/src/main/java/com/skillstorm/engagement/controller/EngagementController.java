package com.skillstorm.engagement.controller;

import com.skillstorm.engagement.dto.CreateEngagementRequest;
import com.skillstorm.engagement.dto.EngagementResponse;
import com.skillstorm.engagement.dto.UpdateEngagementRequest;
import com.skillstorm.engagement.service.EngagementService;
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

@RestController
@RequestMapping("/api/engagements")
public class EngagementController {

    private final EngagementService engagementService;

    public EngagementController(
            EngagementService engagementService) {

        this.engagementService = engagementService;
    }

    @PreAuthorize("hasRole('ENGAGEMENT_MANAGER')")
    @PostMapping
    public ResponseEntity<EngagementResponse> create(
            @Valid @RequestBody CreateEngagementRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        engagementService.createEngagement(request)
                );
    }

    @GetMapping
    public ResponseEntity<List<EngagementResponse>> getAll(
            @AuthenticationPrincipal Jwt jwt) {

        if (isEngagementManager(jwt)) {

            return ResponseEntity.ok(
                    engagementService.getAllEngagements()
            );
        }

        return ResponseEntity.ok(
                engagementService
                        .getEngagementsForCurrentConsultant(
                                jwt.getTokenValue()
                        )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<EngagementResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        if (isEngagementManager(jwt)) {

            return ResponseEntity.ok(
                    engagementService.getEngagementById(id)
            );
        }

        return ResponseEntity.ok(
                engagementService
                        .getEngagementByIdForConsultant(
                                id,
                                jwt.getTokenValue()
                        )
        );
    }

    @PreAuthorize("hasRole('ENGAGEMENT_MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<EngagementResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEngagementRequest request) {

        return ResponseEntity.ok(
                engagementService.updateEngagement(
                        id,
                        request
                )
        );
    }

    @PreAuthorize("hasRole('ENGAGEMENT_MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        engagementService.deleteEngagement(id);

        return ResponseEntity.noContent().build();
    }

    private boolean isEngagementManager(Jwt jwt) {

        List<String> roles =
                jwt.getClaimAsStringList("roles");

        return roles != null
                && roles.contains("ENGAGEMENT_MANAGER");
    }
}