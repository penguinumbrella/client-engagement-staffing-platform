package com.skillstorm.auth_service.Services;

import com.skillstorm.auth_service.Entities.LoginAttempt;
import com.skillstorm.auth_service.Repositories.LoginAttemptRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;

    public LoginAttemptService(
            LoginAttemptRepository loginAttemptRepository
    ) {
        this.loginAttemptRepository = loginAttemptRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAttempt(
            UUID userId,
            String email,
            boolean successful,
            String failureReason
    ) {

        LoginAttempt attempt = new LoginAttempt();

        attempt.setId(UUID.randomUUID());
        attempt.setUserId(userId);
        attempt.setEmail(email);
        attempt.setSuccessful(successful);
        attempt.setFailureReason(failureReason);
        attempt.setAttemptedAt(OffsetDateTime.now());

        loginAttemptRepository.save(attempt);
    }
}