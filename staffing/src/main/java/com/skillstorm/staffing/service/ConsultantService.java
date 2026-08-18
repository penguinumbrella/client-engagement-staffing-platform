package com.skillstorm.staffing.service;

import com.skillstorm.staffing.client.AuthClient;
import com.skillstorm.staffing.dto.AuthUserResponse;
import com.skillstorm.staffing.dto.ConsultantResponse;
import com.skillstorm.staffing.dto.CreateConsultantRequest;
import com.skillstorm.staffing.dto.ProvisionConsultantRequest;
import com.skillstorm.staffing.dto.UpdateConsultantRequest;
import com.skillstorm.staffing.model.Consultant;
import com.skillstorm.staffing.repository.ConsultantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ConsultantService {

    private final ConsultantRepository consultantRepository;
    private final AuthClient authClient;

    public ConsultantService(
        ConsultantRepository consultantRepository,
        AuthClient authClient) {

     this.consultantRepository = consultantRepository;
     this.authClient = authClient;
    }

    public ConsultantResponse createConsultant(
        CreateConsultantRequest request,
        String token) {

        AuthUserResponse authUser =
                authClient.getUserByEmail(
                        request.getEmail(),
                        token
                );
        
        if (consultantRepository.findByUserId(authUser.id()).isPresent()) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A consultant profile already exists for this user"
                );
        }

        if (!authUser.enabled()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "The auth user is disabled"
                );
        }

        if (!"CONSULTANT".equals(authUser.role())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "The auth user must have the CONSULTANT role"
                );
        }

        Consultant consultant = new Consultant(
                request.getName(),
                request.getTitleRole(),
                request.getPrimarySkillArea().getLabel(),
                authUser.id()
        );

        return ConsultantResponse.from(
                consultantRepository.save(consultant)
        );
    }

    public List<ConsultantResponse> getAllConsultants() {
        return consultantRepository.findByActiveTrue()
                .stream()
                .map(ConsultantResponse::from)
                .toList();
    }

    public ConsultantResponse getConsultantById(Long id) {
        return ConsultantResponse.from(
                findActiveOrThrow(id)
        );
    }

    public ConsultantResponse getByAuthUserId(UUID userId) {
        return ConsultantResponse.from(
                findActiveByUserIdOrThrow(userId)
        );
    }

    public ConsultantResponse updateConsultant(
            Long id,
            UpdateConsultantRequest request) {

        Consultant consultant = findActiveOrThrow(id);

        if (request.getName() != null) {
            consultant.setName(request.getName());
        }

        if (request.getTitleRole() != null) {
            consultant.setTitleRole(request.getTitleRole());
        }

        if (request.getPrimarySkillArea() != null) {
            consultant.setPrimarySkillArea(
                    request.getPrimarySkillArea().getLabel()
            );
        }

        return ConsultantResponse.from(
                consultantRepository.save(consultant)
        );
    }

    public void deleteConsultant(Long id) {

        Consultant consultant = findActiveOrThrow(id);

        consultant.setActive(false);

        consultantRepository.save(consultant);
    }

    Consultant findActiveOrThrow(Long id) {

        return consultantRepository.findById(id)
                .filter(Consultant::isActive)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Consultant " + id + " not found"
                        )
                );
    }

    public Consultant findActiveByUserIdOrThrow(UUID userId) {

        return consultantRepository
                .findByUserIdAndActiveTrue(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "No active consultant found for authenticated user"
                        )
                );
    }

    @Transactional
    public ConsultantResponse provisionConsultant(UUID userId, ProvisionConsultantRequest request) {

        return consultantRepository
                .findByUserId(userId)
                .map(ConsultantResponse::from)
                .orElseGet(() -> {

                        Consultant consultant = new Consultant(
                                request.firstName().trim()
                                        + " "
                                        + request.lastName().trim(),
                                request.titleRole().trim(),
                                request.primarySkillArea().getLabel(),
                                userId
                        );

                        Consultant saved =
                                consultantRepository.save(consultant);

                        return ConsultantResponse.from(saved);
                });
    }

}
