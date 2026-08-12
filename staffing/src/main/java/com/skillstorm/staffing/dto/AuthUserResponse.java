package com.skillstorm.staffing.dto;

import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String email,
        String role,
        boolean enabled
) {}