package com.skillstorm.auth_service.Services;

import com.skillstorm.auth_service.Dtos.LoginAttemptResponse;
import com.skillstorm.auth_service.Dtos.LoginMetricsResponse;
import com.skillstorm.auth_service.Repositories.LoginAttemptRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    private final LoginAttemptRepository loginAttemptRepository;

    public AdminService(
            LoginAttemptRepository loginAttemptRepository
    ) {
        this.loginAttemptRepository = loginAttemptRepository;
    }

    public List<LoginAttemptResponse> getRecentLoginAttempts() {

        return loginAttemptRepository
                .findTop100ByOrderByAttemptedAtDesc()
                .stream()
                .map(attempt -> new LoginAttemptResponse(
                        attempt.getId(),
                        attempt.getUserId(),
                        attempt.getEmail(),
                        attempt.isSuccessful(),
                        attempt.getFailureReason(),
                        attempt.getAttemptedAt()
                ))
                .toList();
    }

    public LoginMetricsResponse getLoginMetrics() {

        long successful =
                loginAttemptRepository.countBySuccessfulTrue();

        long failed =
                loginAttemptRepository.countBySuccessfulFalse();

        long total = successful + failed;

        double failureRate =
                total == 0
                        ? 0
                        : ((double) failed / total) * 100;

        return new LoginMetricsResponse(
                total,
                successful,
                failed,
                failureRate
        );
    }
}