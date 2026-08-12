package com.skillstorm.staffing.service;

import com.skillstorm.staffing.client.EngagementClient;
import com.skillstorm.staffing.dto.AssignmentResponse;
import com.skillstorm.staffing.dto.CreateAssignmentRequest;
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

        Assignment assignment = assignmentRepository
                .findByConsultantIdAndEngagementId(request.getConsultantId(), request.getEngagementId())
                .map(existing -> reactivate(existing, request))
                .orElseGet(() -> new Assignment(
                        request.getConsultantId(),
                        request.getEngagementId(),
                        request.getEngagementRole().getLabel(),
                        request.getAssignmentStartDate()
                ));

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
        existing.setActive(true);
        return existing;
    }

    public List<AssignmentResponse> getAssignmentsByConsultant(Long consultantId) {
        Consultant consultant = consultantService.findActiveOrThrow(consultantId);
        return assignmentRepository.findByConsultantIdAndActiveTrue(consultantId).stream()
                .map(a -> AssignmentResponse.from(a, consultant.getName()))
                .toList();
    }

    public List<AssignmentResponse> getAssignmentsByEngagement(Long engagementId) {
        List<Assignment> assignments = assignmentRepository.findByEngagementIdAndActiveTrue(engagementId);

        Map<Long, String> consultantNamesById = consultantRepository
                .findAllById(assignments.stream().map(Assignment::getConsultantId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Consultant::getId, Consultant::getName));

        return assignments.stream()
                .map(a -> AssignmentResponse.from(a, consultantNamesById.get(a.getConsultantId())))
                .toList();
    }

    public void removeAssignment(Long id) {
        Assignment assignment = assignmentRepository.findById(id)
                .filter(Assignment::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment " + id + " not found"));
        assignment.setActive(false);
        assignmentRepository.save(assignment);
    }

    public List<Long> getEngagementIdsForUser(UUID userId) {

        Consultant consultant =
                consultantRepository
                        .findByUserIdAndActiveTrue(userId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "No active consultant linked to user"
                                )
                        );

        return assignmentRepository
                .findByConsultantIdAndActiveTrue(
                        consultant.getId()
                )
                .stream()
                .map(Assignment::getEngagementId)
                .toList();
    }

    public boolean isUserAssignedToEngagement(UUID userId,Long engagementId) {
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
}
