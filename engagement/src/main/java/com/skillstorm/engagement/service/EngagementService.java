package com.skillstorm.engagement.service;

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

    public EngagementService(EngagementRepository engagementRepository) {
        this.engagementRepository = engagementRepository;
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

        return EngagementResponse.from(engagementRepository.save(engagement));
    }

    public void deleteEngagement(Long id) {
        Engagement engagement = findActiveOrThrow(id);
        engagement.setActive(false);
        engagementRepository.save(engagement);
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
