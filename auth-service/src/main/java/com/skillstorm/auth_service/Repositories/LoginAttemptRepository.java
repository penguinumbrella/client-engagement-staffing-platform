package com.skillstorm.auth_service.Repositories;

import com.skillstorm.auth_service.Entities.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoginAttemptRepository
        extends JpaRepository<LoginAttempt, UUID> {

    List<LoginAttempt> findTop100ByOrderByAttemptedAtDesc();

    long countBySuccessfulTrue();

    long countBySuccessfulFalse();
}