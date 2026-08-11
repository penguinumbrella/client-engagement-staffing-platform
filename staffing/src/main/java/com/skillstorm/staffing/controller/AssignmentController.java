package com.skillstorm.staffing.controller;

import com.skillstorm.staffing.dto.AssignmentResponse;
import com.skillstorm.staffing.dto.CascadeAssignmentStatusRequest;
import com.skillstorm.staffing.dto.CreateAssignmentRequest;
import com.skillstorm.staffing.dto.UpdateAssignmentStatusRequest;
import com.skillstorm.staffing.service.AssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping
    public ResponseEntity<AssignmentResponse> assign(@Valid @RequestBody CreateAssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentService.assignConsultant(request));
    }

    @GetMapping("/consultant/{consultantId}")
    public ResponseEntity<List<AssignmentResponse>> getByConsultant(@PathVariable Long consultantId) {
        return ResponseEntity.ok(assignmentService.getAssignmentsByConsultant(consultantId));
    }

    @GetMapping("/engagement/{engagementId}")
    public ResponseEntity<List<AssignmentResponse>> getByEngagement(@PathVariable Long engagementId) {
        return ResponseEntity.ok(assignmentService.getAssignmentsByEngagement(engagementId));
    }

    /**
     * Internal endpoint: called by the engagement service to check whether an engagement has any
     * staffing footprint at all (active or not) before allowing a hard delete.
     */
    @GetMapping("/engagement/{engagementId}/history")
    public ResponseEntity<List<AssignmentResponse>> getHistoryByEngagement(@PathVariable Long engagementId) {
        return ResponseEntity.ok(assignmentService.getAssignmentHistoryByEngagement(engagementId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        assignmentService.removeAssignment(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AssignmentResponse> updateStatus(@PathVariable Long id,
                                                            @Valid @RequestBody UpdateAssignmentStatusRequest request) {
        return ResponseEntity.ok(assignmentService.updateStatus(id, request));
    }

    /** Internal endpoint: called by the engagement service after it commits its own status transition. */
    @PatchMapping("/engagement/{engagementId}/cascade-status")
    public ResponseEntity<Void> cascadeStatus(@PathVariable Long engagementId,
                                               @Valid @RequestBody CascadeAssignmentStatusRequest request) {
        assignmentService.cascadeStatusFromEngagement(engagementId, request.getEngagementStatus());
        return ResponseEntity.noContent().build();
    }

    /** Internal endpoint: called by the engagement service after it cancels an engagement. */
    @DeleteMapping("/engagement/{engagementId}")
    public ResponseEntity<Void> cascadeDelete(@PathVariable Long engagementId) {
        assignmentService.cascadeRemoveFromEngagement(engagementId);
        return ResponseEntity.noContent().build();
    }
}
