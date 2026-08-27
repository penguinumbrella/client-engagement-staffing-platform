package com.skillstorm.auth_service.Dtos;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LoginAttemptResponse(
        UUID id,
        UUID userId,
        String email,
        boolean successful,
        String failureReason,
        OffsetDateTime attemptedAt
) {
}