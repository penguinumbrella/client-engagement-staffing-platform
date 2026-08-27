package com.skillstorm.auth_service.Dtos;

public record LoginMetricsResponse(
        long totalAttempts,
        long successfulAttempts,
        long failedAttempts,
        double failureRate
) {
}