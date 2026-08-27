package com.skillstorm.staffing.service;

import com.skillstorm.staffing.client.AuthClient;
import com.skillstorm.staffing.client.EngagementClient;
import com.skillstorm.staffing.dto.AssignmentResponse;
import com.skillstorm.staffing.dto.AuthUserResponse;
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
    private final AuthClient authClient;
    private final NotificationEventPublisher notificationEventPublisher;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            ConsultantRepository consultantRepository,
            ConsultantService consultantService,
            EngagementClient engagementClient,
            AuthClient authClient,
            NotificationEventPublisher notificationEventPublisher) {

        this.assignmentRepository = assignmentRepository;
        this.consultantRepository = consultantRepository;
        this.consultantService = consultantService;
        this.engagementClient = engagementClient;
        this.authClient = authClient;
        this.notificationEventPublisher = notificationEventPublisher;
    }

    /**
     * All engagement managers, split into "the acting EM" (for attributing
     * broadcast messages, e.g. "Bob staffed Alice...") and "everyone else"
     * (the actual broadcast recipients — EMs can all do the same things,
     * so every EM-triggered action is visible to the rest of the group,
     * minus the one who just did it).
     */
    private record EmBroadcastContext(String actorName, List<AuthUserResponse> others) {
    }

    private EmBroadcastContext resolveEmBroadcastContext(String token, UUID actorId) {

        List<AuthUserResponse> engagementManagers =
                authClient.getUsersByRole("ENGAGEMENT_MANAGER", token);

        String actorName = engagementManagers
                .stream()
                .filter(em -> em.id().equals(actorId))
                .findFirst()
                .map(em -> em.firstName() + " " + em.lastName())
                .orElse("An engagement manager");

        List<AuthUserResponse> others = engagementManagers
                .stream()
                .filter(em -> !em.id().equals(actorId))
                .toList();

        return new EmBroadcastContext(actorName, others);
    }

    private void notifyOtherEngagementManagers(
            EmBroadcastContext context,
            String eventType,
            Long sourceId,
            String title,
            String message) {

        context.others().forEach(em ->
                notificationEventPublisher.publish(new NotificationEvent(
                        eventType,
                        "staffing",
                        sourceId,
                        em.id(),
                        title,
                        message
                ))
        );
    }

    @Transactional
    public AssignmentResponse assignConsultant(
            CreateAssignmentRequest request,
            String token,
            UUID actorId) {

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
                consultant.getUserId(),
                "New assignment",
                "You were staffed on engagement " + saved.getEngagementId()
                        + " as " + saved.getEngagementRole() + "."
        ));

        EmBroadcastContext emContext =
                resolveEmBroadcastContext(token, actorId);

        notifyOtherEngagementManagers(
                emContext,
                "ASSIGNMENT_CREATED",
                saved.getId(),
                "New assignment",
                emContext.actorName() + " staffed " + consultant.getName()
                        + " on engagement " + saved.getEngagementId()
                        + " as " + saved.getEngagementRole() + "."
        );

        return AssignmentResponse.from(
                saved,
                consultant.getName(),
                consultant.getUserId()
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
                                consultant.getName(),
                                consultant.getUserId()
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

        Map<Long, Consultant> consultantsById =
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
                                        c -> c
                                )
                        );

        return assignments
                .stream()
                .map(a -> {
                    Consultant consultant = consultantsById.get(a.getConsultantId());
                    return AssignmentResponse.from(
                            a,
                            consultant != null ? consultant.getName() : null,
                            consultant != null ? consultant.getUserId() : null
                    );
                })
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

        Map<Long, UUID> consultantUserIdsById =
                consultantRepository
                        .findAllById(
                                assignments.stream()
                                        .map(Assignment::getConsultantId)
                                        .distinct()
                                        .toList()
                        )
                        .stream()
                        .collect(Collectors.toMap(Consultant::getId, Consultant::getUserId));

        assignments.forEach(assignment ->
                notificationEventPublisher.publish(new NotificationEvent(
                        "ASSIGNMENT_CANCELLED",
                        "staffing",
                        assignment.getId(),
                        consultantUserIdsById.get(assignment.getConsultantId()),
                        "Assignment cancelled",
                        "Your assignment on engagement " + assignment.getEngagementId()
                                + " was cancelled."
                ))
        );

        log.info(
                "Cascaded deletion of engagement id={} to {} assignment(s)",
                engagementId,
                assignments.size()
        );
    }

    public void removeAssignment(Long id, String token, UUID actorId) {

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

        Assignment saved =
                assignmentRepository.save(assignment);

        Consultant consultant =
                consultantRepository
                        .findById(saved.getConsultantId())
                        .orElse(null);

        notificationEventPublisher.publish(new NotificationEvent(
                "ASSIGNMENT_REMOVED",
                "staffing",
                saved.getId(),
                consultant != null ? consultant.getUserId() : null,
                "Assignment removed",
                "Your assignment on engagement " + saved.getEngagementId()
                        + " was removed."
        ));

        EmBroadcastContext emContext =
                resolveEmBroadcastContext(token, actorId);

        notifyOtherEngagementManagers(
                emContext,
                "ASSIGNMENT_REMOVED",
                saved.getId(),
                "Assignment removed",
                emContext.actorName() + " removed "
                        + (consultant != null ? consultant.getName() : "a consultant")
                        + " from engagement " + saved.getEngagementId() + "."
        );
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
            UpdateAssignmentStatusRequest request,
            String token,
            UUID actorId) {

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

        notificationEventPublisher.publish(new NotificationEvent(
                "ASSIGNMENT_UPDATED",
                "staffing",
                saved.getId(),
                consultant != null ? consultant.getUserId() : null,
                "Assignment status updated",
                "Your assignment on engagement " + saved.getEngagementId()
                        + " was updated to " + saved.getStatus() + "."
        ));

        EmBroadcastContext emContext =
                resolveEmBroadcastContext(token, actorId);

        notifyOtherEngagementManagers(
                emContext,
                "ASSIGNMENT_UPDATED",
                saved.getId(),
                "Assignment status updated",
                emContext.actorName() + " updated "
                        + (consultant != null ? consultant.getName() : "a consultant") + "'s assignment on engagement "
                        + saved.getEngagementId() + " to " + saved.getStatus() + "."
        );

        return AssignmentResponse.from(
                saved,
                consultant != null
                        ? consultant.getName()
                        : null,
                consultant != null
                        ? consultant.getUserId()
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

        Map<Long, UUID> consultantUserIdsById =
                consultantRepository
                        .findAllById(
                                assignments.stream()
                                        .map(Assignment::getConsultantId)
                                        .distinct()
                                        .toList()
                        )
                        .stream()
                        .collect(Collectors.toMap(Consultant::getId, Consultant::getUserId));

        assignments.forEach(assignment ->
                notificationEventPublisher.publish(new NotificationEvent(
                        "ASSIGNMENT_UPDATED",
                        "staffing",
                        assignment.getId(),
                        consultantUserIdsById.get(assignment.getConsultantId()),
                        "Assignment status updated",
                        "Your assignment on engagement " + assignment.getEngagementId()
                                + " was updated to " + target.getLabel() + "."
                ))
        );

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
                                consultant.getName(),
                                consultant.getUserId()
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