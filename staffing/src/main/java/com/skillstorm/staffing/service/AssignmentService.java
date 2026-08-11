package com.skillstorm.staffing.service;

import com.skillstorm.staffing.client.EngagementClient;
import com.skillstorm.staffing.dto.AssignmentResponse;
import com.skillstorm.staffing.dto.CreateAssignmentRequest;
import com.skillstorm.staffing.dto.UpdateAssignmentStatusRequest;
import com.skillstorm.staffing.enums.AssignmentStatus;
import com.skillstorm.staffing.model.Assignment;
import com.skillstorm.staffing.model.Consultant;
import com.skillstorm.staffing.repository.AssignmentRepository;
import com.skillstorm.staffing.repository.ConsultantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentService.class);

    private final AssignmentRepository assignmentRepository;
    private final ConsultantRepository consultantRepository;
    private final ConsultantService consultantService;
    private final EngagementClient engagementClient;

    public AssignmentService(AssignmentRepository assignmentRepository,
                              ConsultantRepository consultantRepository,
                              ConsultantService consultantService,
                              EngagementClient engagementClient) {
        this.assignmentRepository = assignmentRepository;
        this.consultantRepository = consultantRepository;
        this.consultantService = consultantService;
        this.engagementClient = engagementClient;
    }

    @Transactional
    public AssignmentResponse assignConsultant(CreateAssignmentRequest request) {
        Consultant consultant = consultantService.findActiveOrThrow(request.getConsultantId());

        if (!engagementClient.engagementExists(request.getEngagementId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Engagement " + request.getEngagementId() + " not found");
        }

        Assignment assignment = assignmentRepository
                .findByConsultantIdAndEngagementId(request.getConsultantId(), request.getEngagementId())
                .map(existing -> reactivate(existing, request))
                .orElseGet(() -> {
                    Assignment created = new Assignment(
                            request.getConsultantId(),
                            request.getEngagementId(),
                            request.getEngagementRole().getLabel(),
                            request.getAssignmentStartDate()
                    );
                    created.setAssignmentEndDate(request.getAssignmentEndDate());
                    created.setStatus(resolveStatus(request));
                    return created;
                });

        Assignment saved = assignmentRepository.save(assignment);

        log.info("Consultant '{}' (id={}) staffed on engagement id={} as {}",
                consultant.getName(), consultant.getId(), saved.getEngagementId(), saved.getEngagementRole());

        return AssignmentResponse.from(saved, consultant.getName());
    }

    private Assignment reactivate(Assignment existing, CreateAssignmentRequest request) {
        if (existing.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Consultant " + request.getConsultantId() + " is already staffed on engagement " + request.getEngagementId());
        }
        existing.setEngagementRole(request.getEngagementRole().getLabel());
        existing.setAssignmentStartDate(request.getAssignmentStartDate());
        existing.setAssignmentEndDate(request.getAssignmentEndDate());
        existing.setStatus(resolveStatus(request));
        existing.setActive(true);
        return existing;
    }

    private String resolveStatus(CreateAssignmentRequest request) {
        AssignmentStatus status = request.getStatus() != null ? request.getStatus() : AssignmentStatus.ACTIVE;
        return status.getLabel();
    }

    public List<AssignmentResponse> getAssignmentsByConsultant(Long consultantId) {
        Consultant consultant = consultantService.findActiveOrThrow(consultantId);
        return assignmentRepository.findByConsultantIdAndActiveTrue(consultantId).stream()
                .map(a -> AssignmentResponse.from(a, consultant.getName()))
                .toList();
    }

    public List<AssignmentResponse> getAssignmentsByEngagement(Long engagementId) {
        return toResponses(assignmentRepository.findByEngagementIdAndActiveTrue(engagementId));
    }

    /**
     * Every assignment ever created for this engagement, active or not. Used to determine whether
     * an engagement has any real staffing footprint at all (see
     * {@link com.skillstorm.staffing.controller.AssignmentController#getHistoryByEngagement}) —
     * an engagement with any history here should be cancelled, never hard-deleted.
     */
    public List<AssignmentResponse> getAssignmentHistoryByEngagement(Long engagementId) {
        return toResponses(assignmentRepository.findByEngagementId(engagementId));
    }

    private List<AssignmentResponse> toResponses(List<Assignment> assignments) {
        Map<Long, String> consultantNamesById = consultantRepository
                .findAllById(assignments.stream().map(Assignment::getConsultantId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Consultant::getId, Consultant::getName));

        return assignments.stream()
                .map(a -> AssignmentResponse.from(a, consultantNamesById.get(a.getConsultantId())))
                .toList();
    }

    /**
     * Called by the engagement service right after it cancels an engagement. Unlike
     * {@link #cascadeStatusFromEngagement}, this ignores {@code statusOverridden} — a cancelled
     * engagement has no more active work, so every assignment tied to it is cancelled and
     * deactivated regardless of who last touched it.
     */
    @Transactional
    public void cascadeRemoveFromEngagement(Long engagementId) {
        List<Assignment> assignments = assignmentRepository.findByEngagementIdAndActiveTrue(engagementId);

        assignments.forEach(assignment -> {
            assignment.setActive(false);
            assignment.setStatus(AssignmentStatus.CANCELLED.getLabel());
        });
        assignmentRepository.saveAll(assignments);

        log.info("Cascaded deletion of engagement id={} to {} assignment(s)", engagementId, assignments.size());
    }

    public void removeAssignment(Long id) {
        Assignment assignment = assignmentRepository.findById(id)
                .filter(Assignment::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment " + id + " not found"));
        assignment.setActive(false);
        assignment.setStatus(AssignmentStatus.CANCELLED.getLabel());
        assignmentRepository.save(assignment);
    }

    /**
     * Manually setting an assignment's status is a deliberate EM decision, so it's marked
     * overridden and is never touched again by {@link #cascadeStatusFromEngagement}.
     */
    @Transactional
    public AssignmentResponse updateStatus(Long id, UpdateAssignmentStatusRequest request) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment " + id + " not found"));

        assignment.setStatus(request.getStatus().getLabel());
        assignment.setStatusOverridden(true);
        Assignment saved = assignmentRepository.save(assignment);

        Consultant consultant = consultantRepository.findById(saved.getConsultantId()).orElse(null);
        return AssignmentResponse.from(saved, consultant != null ? consultant.getName() : null);
    }

    /**
     * Called by the engagement service right after it commits its own status transition.
     * Only assignments still under system control (not manually overridden) are updated, and
     * "On Hold" is deliberately absent below — a paused engagement says nothing about whether any
     * individual consultant is still working, so it cascades nothing.
     */
    @Transactional
    public void cascadeStatusFromEngagement(Long engagementId, String engagementStatus) {
        AssignmentStatus target = defaultStatusFor(engagementStatus);
        if (target == null) {
            return;
        }

        List<Assignment> assignments =
                assignmentRepository.findByEngagementIdAndActiveTrueAndStatusOverriddenFalse(engagementId);

        assignments.forEach(assignment -> assignment.setStatus(target.getLabel()));
        assignmentRepository.saveAll(assignments);

        log.info("Cascaded engagement id={} status '{}' to {} assignment(s) as '{}'",
                engagementId, engagementStatus, assignments.size(), target.getLabel());
    }

    private AssignmentStatus defaultStatusFor(String engagementStatus) {
        if (engagementStatus == null) {
            return null;
        }
        return switch (engagementStatus.trim().toLowerCase()) {
            case "planned" -> AssignmentStatus.PENDING;
            case "in progress" -> AssignmentStatus.ACTIVE;
            case "completed" -> AssignmentStatus.COMPLETED;
            default -> null; // "On Hold" (and anything unrecognized) cascades nothing
        };
    }
}
