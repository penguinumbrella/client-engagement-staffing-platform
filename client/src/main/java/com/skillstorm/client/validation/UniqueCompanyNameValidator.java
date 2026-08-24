package com.skillstorm.client.validation;

import org.springframework.stereotype.Component;

import com.skillstorm.client.repositories.ClientRepository;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

@Component
public class UniqueCompanyNameValidator implements ConstraintValidator<UniqueCompanyName, String> {

    private final ClientRepository clientRepo;

    public UniqueCompanyNameValidator(ClientRepository clientRepo) {
        this.clientRepo = clientRepo;
    }

    @Override
    public boolean isValid(String companyName, ConstraintValidatorContext context) {
        // Blank values are the concern of @NotBlank, not this constraint.
        if (companyName == null || companyName.isBlank()) {
            return true;
        }

        return !clientRepo.existsByCompanyNameIgnoreCase(companyName);
    }

}
