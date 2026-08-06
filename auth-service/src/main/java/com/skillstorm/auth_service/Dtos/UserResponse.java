package com.skillstorm.auth_service.Dtos;

import java.time.Instant;
import java.util.UUID;

import com.skillstorm.auth_service.Enums.UserRole;

public record UserResponse(
    UUID id,
    String firstName,
    String lastName,
    String email,
    UserRole role,
    boolean enabled,
    Instant createdAt
) {}