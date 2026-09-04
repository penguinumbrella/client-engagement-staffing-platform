package com.skillstorm.auth_service.Config;

import java.io.IOException;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

    private final String frontendLoginUri;

    public OAuth2LoginFailureHandler(
            @Value("${app.oauth2.frontend-redirect-uri}") String frontendRedirectUri) {
        URI callback = URI.create(frontendRedirectUri);
        this.frontendLoginUri = callback.getScheme() + "://" + callback.getAuthority() + "/login";
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {

        log.warn("Google OAuth sign-in failed: {}", exception.getMessage());
        response.sendRedirect(frontendLoginUri + "?error=oauth");
    }
}
