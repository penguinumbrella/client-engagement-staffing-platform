package com.skillstorm.engagement.service;

import com.skillstorm.engagement.client.AuthClient;
import com.skillstorm.engagement.client.ClientClient;
import com.skillstorm.engagement.client.StaffingClient;
import com.skillstorm.engagement.dto.AuthUserResponse;
import com.skillstorm.engagement.dto.CreateEngagementRequest;
import com.skillstorm.engagement.dto.EngagementResponse;
import com.skillstorm.engagement.dto.UpdateEngagementRequest;
import com.skillstorm.engagement.enums.EngagementStatus;
import com.skillstorm.engagement.kafka.NotificationEvent;
import com.skillstorm.engagement.kafka.NotificationEventPublisher;
import com.skillstorm.engagement.model.Engagement;
import com.skillstorm.engagement.repository.EngagementRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class EngagementService {

    private final EngagementRepository engagementRepository;
    private final StaffingClient staffingClient;
    private final ClientClient clientClient;
    private final AuthClient authClient;
    private final NotificationEventPublisher notificationEventPublisher;

    public EngagementService(
            EngagementRepository engagementRepository,
            StaffingClient staffingClient,
            ClientClient clientClient,
            AuthClient authClient,
            NotificationEventPublisher notificationEventPublisher) {

        this.engagementRepository = engagementRepository;
        this.staffingClient = staffingClient;
        this.clientClient = clientClient;
        this.authClient = authClient;
        this.notificationEventPublisher = notificationEventPublisher;
    }

    /**
     * All engagement managers, split into "the acting EM" (for attributing
     * broadcast messages, e.g. "Jane cancelled...") and "everyone else"
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
            Long engagementId,
            String title,
            String message) {

        context.others().forEach(em ->
                notificationEventPublisher.publish(new NotificationEvent(
                        eventType,
                        "engagement",
                        engagementId,
                        em.id(),
                        title,
                        message
                ))
        );
    }

    private void notifyStaffedConsultants(
            List<UUID> consultantUserIds,
            Long engagementId,
            String eventType,
            String title,
            String message) {

        consultantUserIds.forEach(userId ->
                notificationEventPublisher.publish(new NotificationEvent(
                        eventType,
                        "engagement",
                        engagementId,
                        userId,
                        title,
                        message
                ))
        );
    }


    /*
     * CREATE
     *
     * Before saving the engagement, confirm that the
     * referenced client actually exists.
     */
    public EngagementResponse createEngagement(
            CreateEngagementRequest request,
            String token,
            UUID actorId) {

        clientClient.validateClientExists(
                request.getClientId(),
                token
        );

        validateTimeline(
                request.getStartDate(),
                request.getTargetEndDate()
        );

        EngagementStatus status =
                request.getStatus() != null
                        ? request.getStatus()
                        : EngagementStatus.PLANNED;

        Engagement engagement = new Engagement(
                request.getEngagementName(),
                request.getClientId(),
                request.getEngagementType().getLabel(),
                request.getStartDate(),
                request.getTargetEndDate(),
                status.getLabel()
        );

        engagement.setSummary(
                request.getSummary()
        );

        Engagement saved =
                engagementRepository.save(engagement);

        EmBroadcastContext emContext =
                resolveEmBroadcastContext(token, actorId);

        notifyOtherEngagementManagers(
                emContext,
                "ENGAGEMENT_CREATED",
                saved.getId(),
                "New engagement",
                emContext.actorName() + " created \"" + saved.getEngagementName() + "\"."
        );

        return EngagementResponse.from(saved);
    }


    /*
     * MANAGER:
     * View every active engagement.
     */
    public List<EngagementResponse> getAllEngagements() {

        return engagementRepository
                .findByActiveTrue()
                .stream()
                .map(EngagementResponse::from)
                .toList();
    }


    /*
     * MANAGER:
     * Search all active engagements by name/summary.
     */
    public List<EngagementResponse> searchEngagements(String q) {

        return engagementRepository
                .findByActiveTrueAndEngagementNameContainingIgnoreCaseOrActiveTrueAndSummaryContainingIgnoreCase(q, q)
                .stream()
                .map(EngagementResponse::from)
                .toList();
    }


    /*
     * CONSULTANT:
     * Search only engagements assigned to the
     * currently authenticated consultant, by name/summary.
     */
    public List<EngagementResponse> searchEngagementsForCurrentConsultant(String q, String token) {

        String needle = q.toLowerCase();

        return getEngagementsForCurrentConsultant(token)
                .stream()
                .filter(e -> e.getEngagementName().toLowerCase().contains(needle)
                        || (e.getSummary() != null && e.getSummary().toLowerCase().contains(needle)))
                .toList();
    }


    /*
     * CONSULTANT:
     * View only engagements assigned to the
     * currently authenticated consultant.
     */
    public List<EngagementResponse> getEngagementsForCurrentConsultant(
            String token) {

        List<Long> engagementIds =
                staffingClient.getCurrentUserEngagementIds(
                        token
                );

        if (engagementIds.isEmpty()) {
            return List.of();
        }

        return engagementRepository
                .findAllById(engagementIds)
                .stream()
                .filter(Engagement::isActive)
                .map(EngagementResponse::from)
                .toList();
    }


    /*
     * MANAGER:
     * Get any active engagement by ID.
     */
    public EngagementResponse getEngagementById(Long id) {

        return EngagementResponse.from(
                findActiveOrThrow(id)
        );
    }


    /*
     * CONSULTANT:
     * Can only access an engagement if Staffing
     * confirms that the current user is assigned to it.
     */
    public EngagementResponse getEngagementByIdForConsultant(Long engagementId, String token) {

        boolean assigned =
                staffingClient.isCurrentUserAssigned(
                        engagementId,
                        token
                );

        if (!assigned) {

            throw new AccessDeniedException(
                    "You are not assigned to this engagement"
            );
        }

        return getEngagementById(
                engagementId
        );
    }


    /*
     * MANAGER:
     * Get active engagements belonging to a client.
     */
    public List<EngagementResponse> getEngagementsByClientId(Long clientId) {

        return engagementRepository
                .findByClientIdAndActiveTrue(clientId)
                .stream()
                .map(EngagementResponse::from)
                .toList();
    }


    /*
     * UPDATE
     *
     * If the engagement status changes, tell Staffing
     * so its assignments can be updated as well.
     */
    public EngagementResponse updateEngagement(
            Long id,
            UpdateEngagementRequest request,
            String token,
            UUID actorId) {

        Engagement engagement =
                findActiveOrThrow(id);


        if (request.getEngagementName() != null) {

            engagement.setEngagementName(
                    request.getEngagementName()
            );
        }


        if (request.getEngagementType() != null) {

            engagement.setEngagementType(
                    request
                            .getEngagementType()
                            .getLabel()
            );
        }


        if (request.getSummary() != null) {

            engagement.setSummary(
                    request.getSummary()
            );
        }


        /*
         * Cancellation must use the dedicated
         * cancellation endpoint.
         */
        if (
                request.getStatus()
                        == EngagementStatus.CANCELLED
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Use the cancel endpoint to cancel an engagement, not a status update"
            );
        }


        boolean statusChanged =
                request.getStatus() != null
                        &&
                !request
                        .getStatus()
                        .getLabel()
                        .equals(
                                engagement.getStatus()
                        );


        if (request.getStatus() != null) {

            engagement.setStatus(
                    request
                            .getStatus()
                            .getLabel()
            );
        }


        if (request.getStartDate() != null) {

            engagement.setStartDate(
                    request.getStartDate()
            );
        }


        if (request.getTargetEndDate() != null) {

            engagement.setTargetEndDate(
                    request.getTargetEndDate()
            );
        }


        validateTimeline(
                engagement.getStartDate(),
                engagement.getTargetEndDate()
        );


        Engagement saved =
                engagementRepository.save(
                        engagement
                );


        /*
         * Forward the same JWT received from the user
         * to Staffing.
         */
        if (statusChanged) {

            staffingClient.cascadeAssignmentStatus(
                    saved.getId(),
                    saved.getStatus(),
                    token
            );
        }


        EmBroadcastContext emContext =
                resolveEmBroadcastContext(token, actorId);

        notifyOtherEngagementManagers(
                emContext,
                "ENGAGEMENT_UPDATED",
                saved.getId(),
                "Engagement updated",
                emContext.actorName() + " updated \"" + saved.getEngagementName() + "\"."
        );

        notifyStaffedConsultants(
                staffingClient.getStaffedConsultantUserIds(saved.getId(), token),
                saved.getId(),
                "ENGAGEMENT_UPDATED",
                "Engagement updated",
                emContext.actorName() + " updated \"" + saved.getEngagementName()
                        + "\", which you're staffed on."
        );

        return EngagementResponse.from(
                saved
        );
    }


    /*
     * DELETE
     *
     * First remove/cancel remaining staffing assignments,
     * then soft-delete the engagement.
     */
    public void deleteEngagement(
            Long id,
            String token,
            UUID actorId) {

        Engagement engagement =
                findActiveOrThrow(id);


        // Captured before the cascade below deactivates these assignments,
        // since the lookup only returns actively-staffed consultants.
        List<UUID> staffedConsultantUserIds =
                staffingClient.getStaffedConsultantUserIds(id, token);


        staffingClient.cascadeEngagementCancelled(
                id,
                token
        );


        engagement.setActive(false);


        Engagement saved =
                engagementRepository.save(
                        engagement
                );


        EmBroadcastContext emContext =
                resolveEmBroadcastContext(token, actorId);

        notifyOtherEngagementManagers(
                emContext,
                "ENGAGEMENT_DELETED",
                saved.getId(),
                "Engagement removed",
                emContext.actorName() + " removed \"" + saved.getEngagementName() + "\"."
        );

        notifyStaffedConsultants(
                staffedConsultantUserIds,
                saved.getId(),
                "ENGAGEMENT_DELETED",
                "Engagement removed",
                emContext.actorName() + " removed \"" + saved.getEngagementName()
                        + "\", which you were staffed on."
        );
    }


    /*
     * CANCEL
     *
     * Keep the engagement in the database but mark
     * it cancelled and deactivate its assignments.
     */
    public EngagementResponse cancelEngagement(
            Long id,
            String token,
            UUID actorId) {

        Engagement engagement =
                findActiveOrThrow(id);


        if (
                EngagementStatus.COMPLETED
                        .getLabel()
                        .equals(
                                engagement.getStatus()
                        )
                ||
                EngagementStatus.CANCELLED
                        .getLabel()
                        .equals(
                                engagement.getStatus()
                        )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only active engagements can be cancelled"
            );
        }


        engagement.setStatus(
                EngagementStatus.CANCELLED
                        .getLabel()
        );


        Engagement saved =
                engagementRepository.save(
                        engagement
                );


        // Captured before the cascade below deactivates these assignments,
        // since the lookup only returns actively-staffed consultants.
        List<UUID> staffedConsultantUserIds =
                staffingClient.getStaffedConsultantUserIds(saved.getId(), token);


        staffingClient.cascadeEngagementCancelled(
                saved.getId(),
                token
        );


        EmBroadcastContext emContext =
                resolveEmBroadcastContext(token, actorId);

        notifyOtherEngagementManagers(
                emContext,
                "ENGAGEMENT_CANCELLED",
                saved.getId(),
                "Engagement cancelled",
                emContext.actorName() + " cancelled \"" + saved.getEngagementName() + "\"."
        );

        notifyStaffedConsultants(
                staffedConsultantUserIds,
                saved.getId(),
                "ENGAGEMENT_CANCELLED",
                "Engagement cancelled",
                emContext.actorName() + " cancelled \"" + saved.getEngagementName()
                        + "\", which you were staffed on."
        );

        return EngagementResponse.from(
                saved
        );
    }


    /*
     * Find only active engagements.
     */
    private Engagement findActiveOrThrow(
            Long id) {

        return engagementRepository
                .findById(id)
                .filter(
                        Engagement::isActive
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Engagement "
                                        + id
                                        + " not found"
                        )
                );
    }


    /*
     * Ensure the engagement ends on or after
     * its start date.
     */
    private void validateTimeline(
            LocalDate startDate,
            LocalDate targetEndDate) {

        if (
                startDate != null
                &&
                targetEndDate != null
                &&
                startDate.isAfter(
                        targetEndDate
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "startDate must not be after targetEndDate"
            );
        }
    }
}