package com.skillstorm.staffing.service;

import com.skillstorm.staffing.dto.ConsultantResponse;
import com.skillstorm.staffing.dto.CreateConsultantRequest;
import com.skillstorm.staffing.dto.UpdateConsultantRequest;
import com.skillstorm.staffing.model.Consultant;
import com.skillstorm.staffing.repository.ConsultantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ConsultantService {

    private final ConsultantRepository consultantRepository;

    public ConsultantService(ConsultantRepository consultantRepository) {
        this.consultantRepository = consultantRepository;
    }

    public ConsultantResponse createConsultant(CreateConsultantRequest request) {
        Consultant consultant = new Consultant(
                request.getName(),
                request.getTitleRole(),
                request.getPrimarySkillArea().getLabel()
        );
        return ConsultantResponse.from(consultantRepository.save(consultant));
    }

    public List<ConsultantResponse> getAllConsultants() {
        return consultantRepository.findByActiveTrue().stream()
                .map(ConsultantResponse::from)
                .toList();
    }

    public ConsultantResponse getConsultantById(Long id) {
        return ConsultantResponse.from(findActiveOrThrow(id));
    }

    public ConsultantResponse updateConsultant(Long id, UpdateConsultantRequest request) {
        Consultant consultant = findActiveOrThrow(id);

        if (request.getName() != null) {
            consultant.setName(request.getName());
        }
        if (request.getTitleRole() != null) {
            consultant.setTitleRole(request.getTitleRole());
        }
        if (request.getPrimarySkillArea() != null) {
            consultant.setPrimarySkillArea(request.getPrimarySkillArea().getLabel());
        }

        return ConsultantResponse.from(consultantRepository.save(consultant));
    }

    public void deleteConsultant(Long id) {
        Consultant consultant = findActiveOrThrow(id);
        consultant.setActive(false);
        consultantRepository.save(consultant);
    }

    Consultant findActiveOrThrow(Long id) {
        return consultantRepository.findById(id)
                .filter(Consultant::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultant " + id + " not found"));
    }
}
