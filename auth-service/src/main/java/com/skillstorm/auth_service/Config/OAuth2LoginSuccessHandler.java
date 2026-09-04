package com.skillstorm.auth_service.Config;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.skillstorm.auth_service.Entities.User;
import com.skillstorm.auth_service.Enums.AuthProvider;
import com.skillstorm.auth_service.Enums.UserRole;
import com.skillstorm.auth_service.Repositories.UserRepository;
import com.skillstorm.auth_service.Services.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final String frontendRedirectUri;

    public OAuth2LoginSuccessHandler(
            UserRepository userRepository,
            JwtService jwtService,
            @Value("${app.oauth2.frontend-redirect-uri}") String frontendRedirectUri) {

        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.frontendRedirectUri = frontendRedirectUri;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

        String email = oidcUser.getEmail();
        String firstName = oidcUser.getGivenName() != null ? oidcUser.getGivenName() : "";
        String lastName = oidcUser.getFamilyName() != null ? oidcUser.getFamilyName() : "";

        if (email == null || email.isBlank()) {
            URI callback = URI.create(frontendRedirectUri);
            response.sendRedirect(
                    callback.getScheme() + "://" + callback.getAuthority() + "/login?error=oauth");
            return;
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        boolean isNewUser = user == null;

        if (isNewUser) {
            user = new User(
                    firstName,
                    lastName,
                    email,
                    null,
                    UserRole.CONSULTANT,
                    AuthProvider.GOOGLE);

            user = userRepository.save(user);
        }

        JwtService.TokenResult tokenResult = jwtService.generateAccessToken(user);

        String redirectUrl = frontendRedirectUri
                + "?token=" + URLEncoder.encode(tokenResult.accessToken(), StandardCharsets.UTF_8)
                + "&onboarding=" + isNewUser;

        response.sendRedirect(redirectUrl);
    }
}
