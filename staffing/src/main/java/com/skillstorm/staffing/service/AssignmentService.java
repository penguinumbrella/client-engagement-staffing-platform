package com.skillstorm.staffing.service;

import com.skillstorm.staffing.client.EngagementClient;
import com.skillstorm.staffing.dto.AssignmentResponse;
import com.skillstorm.staffing.dto.CreateAssignmentRequest;
import com.skillstorm.staffing.dto.UpdateAssignmentStatusRequest;
import com.skillstorm.staffing.enums.AssignmentStatus;
import com.skillstorm.staffing.kafka.NotificationEvent;
import com.skillstorm.staffing.kafka.NotificationEventPublisher;
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
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AssignmentService {

    private static final Logger log =
            LoggerFactory.getLogger(AssignmentService.class);

    private final AssignmentRepository assignmentRepository;
    private final ConsultantRepository consultantRepository;
    private final ConsultantService consultantService;
    private final EngagementClient engagementClient;
    private final NotificationEventPublisher notificationEventPublisher;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            ConsultantRepository consultantRepository,
            ConsultantService consultantService,
            EngagementClient engagementClient,
            NotificationEventPublisher notificationEventPublisher) {

        this.assignmentRepository = assignmentRepository;
        this.consultantRepository = consultantRepository;
        this.consultantService = consultantService;
        this.engagementClient = engagementClient;
        this.notificationEventPublisher = notificationEventPublisher;
    }

    @Transactional
    public AssignmentResponse assignConsultant(
            CreateAssignmentRequest request,
            String token) {

        Consultant consultant =
                consultantService.findActiveOrThrow(
                        request.getConsultantId()
                );

        if (!engagementClient.engagementExists(
                request.getEngagementId(),
                token)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Engagement "
                            + request.getEngagementId()
                            + " not found"
            );
        }

        Assignment assignment =
                assignmentRepository
                        .findByConsultantIdAndEngagementId(
                                request.getConsultantId(),
                                request.getEngagementId()
                        )
                        .map(existing ->
                                reactivate(existing, request)
                        )
                        .orElseGet(() -> {

                            Assignment created =
                                    new Assignment(
                                            request.getConsultantId(),
                                            request.getEngagementId(),
                                            request.getEngagementRole().getLabel(),
                                            request.getAssignmentStartDate()
                                    );

                            created.setAssignmentEndDate(
                                    request.getAssignmentEndDate()
                            );

                            created.setStatus(
                                    resolveStatus(request)
                            );

                            return created;
                        });

        Assignment saved =
                assignmentRepository.save(assignment);

        log.info(
                "Consultant '{}' (id={}) staffed on engagement id={} as {}",
                consultant.getName(),
                consultant.getId(),
                saved.getEngagementId(),
                saved.getEngagementRole()
        );

        notificationEventPublisher.publish(new NotificationEvent(
                "ASSIGNMENT_CREATED",
                "staffing",
                saved.getId(),
                saved.getConsultantId(),
                "New assignment",
                "You were staffed on engagement " + saved.getEngagementId()
                        + " as " + saved.getEngagementRole() + "."
        ));

        return AssignmentResponse.from(
                saved,
                consultant.getName()
        );
    }

    private Assignment reactivate(
            Assignment existing,
            CreateAssignmentRequest request) {

        if (existing.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Consultant "
                            + request.getConsultantId()
                            + " is already staffed on engagement "
                            + request.getEngagementId()
            );
        }

        existing.setEngagementRole(
                request.getEngagementRole().getLabel()
        );

        existing.setAssignmentStartDate(
                request.getAssignmentStartDate()
        );

        existing.setAssignmentEndDate(
                request.getAssignmentEndDate()
        );

        existing.setStatus(
                resolveStatus(request)
        );

        existing.setActive(true);

        return existing;
    }

    private String resolveStatus(
            CreateAssignmentRequest request) {

        AssignmentStatus status =
                request.getStatus() != null
                        ? request.getStatus()
                        : AssignmentStatus.ACTIVE;

        return status.getLabel();
    }

    public List<AssignmentResponse> getAssignmentsByConsultant(
            Long consultantId) {

        Consultant consultant =
                consultantService.findActiveOrThrow(
                        consultantId
                );

        return assignmentRepository
                .findByConsultantIdAndActiveTrue(
                        consultantId
                )
                .stream()
                .map(a ->
                        AssignmentResponse.from(
                                a,
                                consultant.getName()
                        )
                )
                .toList();
    }

    public List<AssignmentResponse> getAssignmentsByEngagement(
            Long engagementId) {

        return toResponses(
                assignmentRepository
                        .findByEngagementIdAndActiveTrue(
                                engagementId
                        )
        );
    }

    /**
     * Every assignment ever created for this engagement,
     * active or inactive.
     */
    public List<AssignmentResponse> getAssignmentHistoryByEngagement(
            Long engagementId) {

        return toResponses(
                assignmentRepository
                        .findByEngagementId(
                                engagementId
                        )
        );
    }

    private List<AssignmentResponse> toResponses(
            List<Assignment> assignments) {

        Map<Long, String> consultantNamesById =
                consultantRepository
                        .findAllById(
                                assignments
                                        .stream()
                                        .map(
                                                Assignment::getConsultantId
                                        )
                                        .distinct()
                                        .toList()
                        )
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Consultant::getId,
                                        Consultant::getName
                                )
                        );

        return assignments
                .stream()
                .map(a ->
                        AssignmentResponse.from(
                                a,
                                consultantNamesById.get(
                                        a.getConsultantId()
                                )
                        )
                )
                .toList();
    }

    /**
     * Called after an engagement is cancelled or deleted.
     * All active assignments are cancelled and deactivated.
     */
    @Transactional
    public void cascadeRemoveFromEngagement(
            Long engagementId) {

        List<Assignment> assignments =
                assignmentRepository
                        .findByEngagementIdAndActiveTrue(
                                engagementId
                        );

        assignments.forEach(assignment -> {
            assignment.setActive(false);
            assignment.setStatus(
                    AssignmentStatus.CANCELLED.getLabel()
            );
        });

        assignmentRepository.saveAll(assignments);

        log.info(
                "Cascaded deletion of engagement id={} to {} assignment(s)",
                engagementId,
                assignments.size()
        );
    }

    public void removeAssignment(Long id) {

        Assignment assignment =
                assignmentRepository
                        .findById(id)
                        .filter(Assignment::isActive)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Assignment "
                                                + id
                                                + " not found"
                                )
                        );

        assignment.setActive(false);

        assignment.setStatus(
                AssignmentStatus.CANCELLED.getLabel()
        );

        assignmentRepository.save(assignment);
    }

    /*
     * Used by /api/assignments/me/engagement-ids.
     * Maps the authenticated user's JWT subject UUID
     * to their consultant record.
     */
    public List<Long> getEngagementIdsForUser(
            UUID userId) {

        Consultant consultant =
                consultantRepository
                        .findByUserIdAndActiveTrue(userId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "No active consultant linked to authenticated user"
                                )
                        );

        return assignmentRepository
                .findByConsultantIdAndActiveTrue(
                        consultant.getId()
                )
                .stream()
                .map(
                        Assignment::getEngagementId
                )
                .toList();
    }

    /*
     * Checks whether the authenticated consultant
     * is assigned to a specific engagement.
     */
    public boolean isUserAssignedToEngagement(
            UUID userId,
            Long engagementId) {

        Consultant consultant =
                consultantRepository
                        .findByUserIdAndActiveTrue(userId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "No active consultant linked to authenticated user"
                                )
                        );

        return assignmentRepository
                .existsByConsultantIdAndEngagementIdAndActiveTrue(
                        consultant.getId(),
                        engagementId
                );
    }

    /**
     * Manually setting an assignment status is an
     * Engagement Manager decision, so it is marked
     * overridden and will not be changed by future
     * engagement status cascades.
     */
    @Transactional
    public AssignmentResponse updateStatus(
            Long id,
            UpdateAssignmentStatusRequest request) {

        Assignment assignment =
                assignmentRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Assignment "
                                                + id
                                                + " not found"
                                )
                        );

        assignment.setStatus(
                request.getStatus().getLabel()
        );

        assignment.setStatusOverridden(true);

        Assignment saved =
                assignmentRepository.save(assignment);

        Consultant consultant =
                consultantRepository
                        .findById(
                                saved.getConsultantId()
                        )
                        .orElse(null);

        return AssignmentResponse.from(
                saved,
                consultant != null
                        ? consultant.getName()
                        : null
        );
    }

    /**
     * Called after an engagement changes status.
     * Only assignments that have not been manually
     * overridden are updated.
     */
    @Transactional
    public void cascadeStatusFromEngagement(
            Long engagementId,
            String engagementStatus) {

        AssignmentStatus target =
                defaultStatusFor(
                        engagementStatus
                );

        if (target == null) {
            return;
        }

        List<Assignment> assignments =
                assignmentRepository
                        .findByEngagementIdAndActiveTrueAndStatusOverriddenFalse(
                                engagementId
                        );

        assignments.forEach(
                assignment ->
                        assignment.setStatus(
                                target.getLabel()
                        )
        );

        assignmentRepository.saveAll(assignments);

        log.info(
                "Cascaded engagement id={} status '{}' to {} assignment(s) as '{}'",
                engagementId,
                engagementStatus,
                assignments.size(),
                target.getLabel()
        );
    }

    private AssignmentStatus defaultStatusFor(
            String engagementStatus) {

        if (engagementStatus == null) {
            return null;
        }

        return switch (
                engagementStatus
                        .trim()
                        .toLowerCase()
        ) {
            case "planned" ->
                    AssignmentStatus.PENDING;

            case "in progress" ->
                    AssignmentStatus.ACTIVE;

            case "completed" ->
                    AssignmentStatus.COMPLETED;

            default ->
                    null;
        };
    }
    public List<AssignmentResponse> getAssignmentsForUser(UUID userId) {

        Consultant consultant =
                consultantRepository
                        .findByUserIdAndActiveTrue(userId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "No active consultant linked to authenticated user"
                                )
                        );

        return assignmentRepository
                .findByConsultantIdAndActiveTrue(
                        consultant.getId()
                )
                .stream()
                .map(assignment ->
                        AssignmentResponse.from(
                                assignment,
                                consultant.getName()
                        )
                )
                .toList();
    }

    public List<AssignmentResponse> getTeamForCurrentUser(UUID userId, Long engagementId) {

        boolean assigned =
                isUserAssignedToEngagement(
                        userId,
                        engagementId
                );

        if (!assigned) {

                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You are not assigned to this engagement"
                );
        }

        return getAssignmentsByEngagement(
                engagementId
        );
    }
}