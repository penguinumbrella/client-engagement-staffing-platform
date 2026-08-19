package com.skillstorm.auth_service.Dtos;

import com.skillstorm.auth_service.Enums.UserRole;

import java.util.UUID;

public record CreateUserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        UserRole role,
        boolean enabled
) {
}