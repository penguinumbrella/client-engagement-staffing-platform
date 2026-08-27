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
import java.util.UUID;

@RestController
@RequestMapping("/api/engagements")
public class EngagementController {

    private final EngagementService engagementService;


    public EngagementController(
            EngagementService engagementService) {

        this.engagementService =
                engagementService;
    }


    /*
     * ENGAGEMENT MANAGER
     *
     * Create an engagement.
     *
     * Pass the JWT to the service because Engagement
     * needs to call Client Service to validate clientId.
     */
    @PreAuthorize("hasRole('ENGAGEMENT_MANAGER')")
    @PostMapping
    public ResponseEntity<EngagementResponse> create(@Valid @RequestBody CreateEngagementRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity
                .status(
                        HttpStatus.CREATED
                )
                .body(
                        engagementService
                                .createEngagement(
                                        request,
                                        jwt.getTokenValue(),
                                        UUID.fromString(jwt.getSubject())
                                )
                );
    }


    /*
     * MANAGER:
     * sees all engagements.
     *
     * CONSULTANT:
     * sees only engagements they are staffed on.
     */
    @GetMapping
    public ResponseEntity<List<EngagementResponse>> getAll(@AuthenticationPrincipal Jwt jwt) {
        if (isEngagementManager(jwt)) {

            return ResponseEntity.ok(
                    engagementService
                            .getAllEngagements()
            );
        }


        return ResponseEntity.ok(
                engagementService
                        .getEngagementsForCurrentConsultant(jwt.getTokenValue())
        );
    }


    /*
     * MANAGER:
     * can view any engagement.
     *
     * CONSULTANT:
     * can only view an engagement they're assigned to.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EngagementResponse> getById(@PathVariable Long id,@AuthenticationPrincipal Jwt jwt) {

        if (isEngagementManager(jwt)) {

            return ResponseEntity.ok(
                    engagementService
                            .getEngagementById(id)
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


    /*
     * ENGAGEMENT MANAGER
     */
    @PreAuthorize("hasRole('ENGAGEMENT_MANAGER')")
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<EngagementResponse>> getByClient(@PathVariable Long clientId) {

        return ResponseEntity.ok(
                engagementService
                        .getEngagementsByClientId(clientId)
        );
    }


    /*
     * ENGAGEMENT MANAGER
     *
     * Pass JWT because a status change may need to
     * cascade to Staffing.
     */
    @PreAuthorize("hasRole('ENGAGEMENT_MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<EngagementResponse> update(@PathVariable Long id,
            @Valid @RequestBody
            UpdateEngagementRequest request,
            @AuthenticationPrincipal
            Jwt jwt) {

        return ResponseEntity.ok(
                engagementService
                        .updateEngagement(
                                id,
                                request,
                                jwt.getTokenValue()
                        )
        );
    }


    /*
     * ENGAGEMENT MANAGER
     *
     * Pass JWT because Staffing must remove/cancel
     * assignments before the engagement is deleted.
     */
    @PreAuthorize("hasRole('ENGAGEMENT_MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {

        engagementService.deleteEngagement(
                id,
                jwt.getTokenValue()
        );

        return ResponseEntity
                .noContent()
                .build();
    }


    /*
     * ENGAGEMENT MANAGER
     *
     * Pass JWT because cancellation must cascade
     * to Staffing.
     */
    @PreAuthorize(
            "hasRole('ENGAGEMENT_MANAGER')"
    )
    @PostMapping("/{id}/cancel")
    public ResponseEntity<EngagementResponse> cancel(
            @PathVariable
            Long id,

            @AuthenticationPrincipal
            Jwt jwt) {

        return ResponseEntity.ok(
                engagementService
                        .cancelEngagement(
                                id,
                                jwt.getTokenValue()
                        )
        );
    }


    /*
     * Determines whether the authenticated user
     * has the Engagement Manager role.
     */
    private boolean isEngagementManager(Jwt jwt) {

        List<String> roles =
                jwt.getClaimAsStringList(
                        "roles"
                );

        return roles != null
                &&
                roles.contains(
                        "ENGAGEMENT_MANAGER"
                );
    }
}