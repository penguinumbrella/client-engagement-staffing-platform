package com.skillstorm.auth_service.Controllers;

import com.skillstorm.auth_service.Dtos.LoginAttemptResponse;
import com.skillstorm.auth_service.Dtos.LoginMetricsResponse;
import com.skillstorm.auth_service.Services.AdminService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(
            AdminService adminService
    ) {
        this.adminService = adminService;
    }

    @GetMapping("/login-attempts")
    public List<LoginAttemptResponse> getLoginAttempts() {
        return adminService.getRecentLoginAttempts();
    }

    @GetMapping("/login-metrics")
    public LoginMetricsResponse getLoginMetrics() {
        return adminService.getLoginMetrics();
    }
}