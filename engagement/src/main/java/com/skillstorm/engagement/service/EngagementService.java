package com.skillstorm.engagement.service;

import com.skillstorm.engagement.client.StaffingClient;
import com.skillstorm.engagement.dto.CreateEngagementRequest;
import com.skillstorm.engagement.dto.EngagementResponse;
import com.skillstorm.engagement.dto.UpdateEngagementRequest;
import com.skillstorm.engagement.enums.EngagementStatus;
import com.skillstorm.engagement.model.Engagement;
import com.skillstorm.engagement.repository.EngagementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class EngagementService {

    private final EngagementRepository engagementRepository;
    private final StaffingClient staffingClient;

    public EngagementService(EngagementRepository engagementRepository, StaffingClient staffingClient) {
        this.engagementRepository = engagementRepository;
        this.staffingClient = staffingClient;
    }

    public EngagementResponse createEngagement(CreateEngagementRequest request) {
        validateTimeline(request.getStartDate(), request.getTargetEndDate());

        EngagementStatus status = request.getStatus() != null ? request.getStatus() : EngagementStatus.PLANNED;

        Engagement engagement = new Engagement(
                request.getEngagementName(),
                request.getClientId(),
                request.getEngagementType().getLabel(),
                request.getStartDate(),
                request.getTargetEndDate(),
                status.getLabel()
        );
        engagement.setSummary(request.getSummary());

        return EngagementResponse.from(engagementRepository.save(engagement));
    }

    public List<EngagementResponse> getAllEngagements() {
        return engagementRepository.findByActiveTrue().stream()
                .map(EngagementResponse::from)
                .toList();
    }

    public EngagementResponse getEngagementById(Long id) {
        return EngagementResponse.from(findActiveOrThrow(id));
    }

    public List<EngagementResponse> getEngagementsByClientId(Long clientId) {
        return engagementRepository.findByClientIdAndActiveTrue(clientId).stream()
                .map(EngagementResponse::from)
                .toList();
    }

    public EngagementResponse updateEngagement(Long id, UpdateEngagementRequest request) {
        Engagement engagement = findActiveOrThrow(id);

        if (request.getEngagementName() != null) {
            engagement.setEngagementName(request.getEngagementName());
        }
        if (request.getEngagementType() != null) {
            engagement.setEngagementType(request.getEngagementType().getLabel());
        }
        if (request.getSummary() != null) {
            engagement.setSummary(request.getSummary());
        }
        if (request.getStatus() == EngagementStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use the cancel endpoint to cancel an engagement, not a status update");
        }

        boolean statusChanged = request.getStatus() != null
                && !request.getStatus().getLabel().equals(engagement.getStatus());
        if (request.getStatus() != null) {
            engagement.setStatus(request.getStatus().getLabel());
        }
        if (request.getStartDate() != null) {
            engagement.setStartDate(request.getStartDate());
        }
        if (request.getTargetEndDate() != null) {
            engagement.setTargetEndDate(request.getTargetEndDate());
        }

        validateTimeline(engagement.getStartDate(), engagement.getTargetEndDate());

        Engagement saved = engagementRepository.save(engagement);

        if (statusChanged) {
            staffingClient.cascadeAssignmentStatus(saved.getId(), saved.getStatus());
        }

        return EngagementResponse.from(saved);
    }

    /**
     * Hard delete is reserved for engagements with zero staffing footprint, ever — anything with
     * assignment history (even fully cancelled) had a real-world effect and must be cancelled
     * instead so that history is preserved.
     */
    public void deleteEngagement(Long id) {
        Engagement engagement = findActiveOrThrow(id);

        if (!staffingClient.getAssignmentHistory(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This engagement has assignment history and cannot be deleted — cancel it instead");
        }

        engagement.setActive(false);
        engagementRepository.save(engagement);
    }

    /**
     * Cancellation is the everyday "this isn't happening" action — available on any engagement
     * that isn't already finished, keeps the record (and its history) intact, and cascades a
     * cancel/deactivate to any remaining assignments.
     */
    public EngagementResponse cancelEngagement(Long id) {
        Engagement engagement = findActiveOrThrow(id);

        if (EngagementStatus.COMPLETED.getLabel().equals(engagement.getStatus())
                || EngagementStatus.CANCELLED.getLabel().equals(engagement.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only active engagements can be cancelled");
        }

        engagement.setStatus(EngagementStatus.CANCELLED.getLabel());
        Engagement saved = engagementRepository.save(engagement);

        staffingClient.cascadeEngagementCancelled(saved.getId());

        return EngagementResponse.from(saved);
    }

    private Engagement findActiveOrThrow(Long id) {
        return engagementRepository.findById(id)
                .filter(Engagement::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Engagement " + id + " not found"));
    }

    private void validateTimeline(LocalDate startDate, LocalDate targetEndDate) {
        if (startDate.isAfter(targetEndDate)) {
            throw new IllegalArgumentException("startDate must not be after targetEndDate");
        }
    }
}
