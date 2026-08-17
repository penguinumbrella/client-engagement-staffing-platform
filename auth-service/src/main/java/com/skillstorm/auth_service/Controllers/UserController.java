package com.skillstorm.auth_service.Controllers;

import com.skillstorm.auth_service.Dtos.UserResponse;
import com.skillstorm.auth_service.Services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
    public ResponseEntity<UserResponse> getUserByEmail(
            @RequestParam String email) {

        return ResponseEntity.ok(
                authService.getUserByEmail(email)
        );
    }
}