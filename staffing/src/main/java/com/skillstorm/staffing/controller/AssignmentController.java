package com.skillstorm.staffing.controller;

import com.skillstorm.staffing.dto.AssignmentResponse;
import com.skillstorm.staffing.dto.CreateAssignmentRequest;
import com.skillstorm.staffing.service.AssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping
    public ResponseEntity<AssignmentResponse> assign(
            @Valid @RequestBody CreateAssignmentRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        assignmentService.assignConsultant(
                                request,
                                jwt.getTokenValue()
                        )
                );
    }

    @GetMapping("/consultant/{consultantId}")
    public ResponseEntity<List<AssignmentResponse>> getByConsultant(@PathVariable Long consultantId) {
        return ResponseEntity.ok(assignmentService.getAssignmentsByConsultant(consultantId));
    }

    @GetMapping("/engagement/{engagementId}")
    public ResponseEntity<List<AssignmentResponse>> getByEngagement(@PathVariable Long engagementId) {
        return ResponseEntity.ok(assignmentService.getAssignmentsByEngagement(engagementId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        assignmentService.removeAssignment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/engagement-ids")
    public ResponseEntity<List<Long>> getMyEngagementIds(
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId =
                UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(
                assignmentService.getEngagementIdsForUser(userId)
        );
    }

    @GetMapping("/me/engagements/{engagementId}/exists")
    public ResponseEntity<Boolean> isAssignedToEngagement(@PathVariable Long engagementId, @AuthenticationPrincipal Jwt jwt) {

        UUID userId =
                UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(
                assignmentService.isUserAssignedToEngagement(
                        userId,
                        engagementId
                )
        );
    }
}
