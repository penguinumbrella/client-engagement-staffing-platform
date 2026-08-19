package com.skillstorm.auth_service.Controllers;

import com.skillstorm.auth_service.Dtos.CreateUserRequest;
import com.skillstorm.auth_service.Dtos.CreateUserResponse;
import com.skillstorm.auth_service.Dtos.UserResponse;
import com.skillstorm.auth_service.Services.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @PreAuthorize("hasRole('ENGAGEMENT_MANAGER')")
    @GetMapping("/by-email")
    public ResponseEntity<UserResponse> getUserByEmail(@RequestParam String email) {

        return ResponseEntity.ok(
                authService.getUserByEmail(email)
        );
    }

    @PreAuthorize("hasRole('ENGAGEMENT_MANAGER')")
    @PostMapping
    public ResponseEntity<CreateUserResponse> createUser(
        @Valid @RequestBody CreateUserRequest request,
        @AuthenticationPrincipal Jwt jwt) {

    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                    authService.createUser(
                            request,
                            jwt.getTokenValue()
                    )
            );
    }
}