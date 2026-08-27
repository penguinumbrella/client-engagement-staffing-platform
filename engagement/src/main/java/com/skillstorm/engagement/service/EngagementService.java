package com.skillstorm.engagement.service;

import com.skillstorm.engagement.client.ClientClient;
import com.skillstorm.engagement.client.StaffingClient;
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
    private final NotificationEventPublisher notificationEventPublisher;

    public EngagementService(
            EngagementRepository engagementRepository,
            StaffingClient staffingClient,
            ClientClient clientClient,
            NotificationEventPublisher notificationEventPublisher) {

        this.engagementRepository = engagementRepository;
        this.staffingClient = staffingClient;
        this.clientClient = clientClient;
        this.notificationEventPublisher = notificationEventPublisher;
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
            UUID ownerId) {

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

        engagement.setOwnerId(ownerId);

        Engagement saved =
                engagementRepository.save(engagement);

        notificationEventPublisher.publish(new NotificationEvent(
                "ENGAGEMENT_CREATED",
                "engagement",
                saved.getId(),
                saved.getOwnerId(),
                "New engagement",
                "Engagement \"" + saved.getEngagementName() + "\" was created."
        ));

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
            String token) {

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


        notificationEventPublisher.publish(new NotificationEvent(
                "ENGAGEMENT_UPDATED",
                "engagement",
                saved.getId(),
                saved.getOwnerId(),
                "Engagement updated",
                "Engagement \"" + saved.getEngagementName() + "\" was updated."
        ));


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
            String token) {

        Engagement engagement =
                findActiveOrThrow(id);


        staffingClient.cascadeEngagementCancelled(
                id,
                token
        );


        engagement.setActive(false);


        Engagement saved =
                engagementRepository.save(
                        engagement
                );


        notificationEventPublisher.publish(new NotificationEvent(
                "ENGAGEMENT_DELETED",
                "engagement",
                saved.getId(),
                saved.getOwnerId(),
                "Engagement removed",
                "Engagement \"" + saved.getEngagementName() + "\" was removed."
        ));
    }


    /*
     * CANCEL
     *
     * Keep the engagement in the database but mark
     * it cancelled and deactivate its assignments.
     */
    public EngagementResponse cancelEngagement(
            Long id,
            String token) {

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


        staffingClient.cascadeEngagementCancelled(
                saved.getId(),
                token
        );


        notificationEventPublisher.publish(new NotificationEvent(
                "ENGAGEMENT_CANCELLED",
                "engagement",
                saved.getId(),
                saved.getOwnerId(),
                "Engagement cancelled",
                "Engagement \"" + saved.getEngagementName() + "\" was cancelled."
        ));


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