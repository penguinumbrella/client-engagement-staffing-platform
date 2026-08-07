package com.skillstorm.auth_service.Dtos;

public record AuthResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    UserResponse user
) {}