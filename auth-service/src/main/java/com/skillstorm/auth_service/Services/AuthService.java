package com.skillstorm.auth_service.Services;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.skillstorm.auth_service.Dtos.AuthResponse;
import com.skillstorm.auth_service.Dtos.CreateUserRequest;
import com.skillstorm.auth_service.Dtos.CreateUserResponse;
import com.skillstorm.auth_service.Dtos.LoginRequest;
import com.skillstorm.auth_service.Dtos.ProvisionConsultantRequest;
import com.skillstorm.auth_service.Dtos.RegisterRequest;
import com.skillstorm.auth_service.Dtos.UserResponse;
import com.skillstorm.auth_service.Entities.User;
import com.skillstorm.auth_service.Enums.UserRole;
import com.skillstorm.auth_service.Exceptions.DuplicateEmailException;
import com.skillstorm.auth_service.Repositories.UserRepository;
import com.skillstorm.auth_service.clients.StaffingClient;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final StaffingClient staffingClient;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        StaffingClient staffingClient) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.staffingClient = staffingClient;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        User savedUser = createUserAccount(
            request.firstName(),
            request.lastName(),
            request.email(),
            request.password(),
            UserRole.CONSULTANT
        );

        AuthResponse authResponse =
                createAuthResponse(savedUser);

        staffingClient.provisionConsultant(
                new ProvisionConsultantRequest(
                        savedUser.getFirstName(),
                        savedUser.getLastName(),
                        request.titleRole(),
                        request.primarySkillArea()
                ),
                authResponse.accessToken()
        );

        return authResponse;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        Authentication authenticationRequest =
                new UsernamePasswordAuthenticationToken(
                        normalizedEmail,
                        request.password());

        Authentication authenticatedUser =
                authenticationManager.authenticate(authenticationRequest);

        User user = userRepository
                .findByEmailIgnoreCase(authenticatedUser.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User could not be found after authentication."));

        return createAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found."
                        )
                );

        return toUserResponse(user);
    }

    private AuthResponse createAuthResponse(User user) {
        JwtService.TokenResult tokenResult =
                jwtService.generateAccessToken(user);

        return new AuthResponse(
                tokenResult.accessToken(),
                "Bearer",
                tokenResult.expiresIn(),
                toUserResponse(user));
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt());
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Email cannot be null.");
        }

        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {

        String normalizedEmail = normalizeEmail(email);

        User user = userRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found: " + normalizedEmail
                        )
                );

        return toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByRole(UserRole role) {

        return userRepository
                .findByRole(role)
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    private User createUserAccount(
        String firstName,
        String lastName,
        String email,
        String password,
        UserRole role) {

        String normalizedEmail =
                normalizeEmail(email);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
                throw new DuplicateEmailException(normalizedEmail);
        }

        String passwordHash =
                passwordEncoder.encode(password);

        User user = new User(
                firstName.trim(),
                lastName.trim(),
                normalizedEmail,
                passwordHash,
                role
        );

        return userRepository.save(user);
    }

    @Transactional
    public CreateUserResponse createUser(
        CreateUserRequest request,
        String managerToken) {

        if (request.role() == UserRole.CONSULTANT) {

                if (request.titleRole() == null ||
                        request.titleRole().isBlank()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "titleRole is required for consultants"
                );
                }

                if (request.primarySkillArea() == null ||
                        request.primarySkillArea().isBlank()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "primarySkillArea is required for consultants"
                );
                }
        }

        // Create the user in auth_db
        User savedUser = createUserAccount(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.password(),
                request.role()
        );

        if (savedUser.getRole() == UserRole.CONSULTANT) {

                staffingClient.provisionConsultantByManager(
                        savedUser.getId(),
                        new ProvisionConsultantRequest(
                                savedUser.getFirstName(),
                                savedUser.getLastName(),
                                request.titleRole(),
                                request.primarySkillArea()
                        ),
                        managerToken
                );
        }

        return new CreateUserResponse(
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.isEnabled()
        );
    }
}
