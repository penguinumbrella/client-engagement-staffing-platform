package com.skillstorm.auth_service.Dtos;

import java.time.Instant;
import java.util.UUID;

import com.skillstorm.auth_service.Entities.User;
import com.skillstorm.auth_service.Enums.UserRole;

public record UserResponse(
    UUID id,
    String firstName,
    String lastName,
    String email,
    UserRole role,
    boolean enabled,
    Instant createdAt
) {

    public static UserResponse from(User user) {
            return new UserResponse(
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getRole(),
                    user.isEnabled(),
                    user.getCreatedAt()
            );
        }
}