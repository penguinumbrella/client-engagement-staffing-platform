package com.skillstorm.auth_service.Config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/*
 * oauth2Login is configured as STATELESS, so the default HttpSession store
 * cannot keep the authorization request between /oauth2/authorization/google
 * and the CloudFront callback. Persist it in a short-lived cookie instead.
 */
@Component
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    static final String COOKIE_NAME = "oauth2_auth_request";
    private static final int COOKIE_MAX_AGE_SECONDS = 180;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return readCookie(request);
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (authorizationRequest == null) {
            clearCookie(request, response);
            return;
        }

        writeCookie(request, response, serialize(authorizationRequest), COOKIE_MAX_AGE_SECONDS);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response) {

        OAuth2AuthorizationRequest authorizationRequest = readCookie(request);
        clearCookie(request, response);
        return authorizationRequest;
    }

    private static OAuth2AuthorizationRequest readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return deserialize(cookie.getValue());
            }
        }
        return null;
    }

    private static void writeCookie(
            HttpServletRequest request,
            HttpServletResponse response,
            String value,
            int maxAge) {

        Cookie cookie = new Cookie(COOKIE_NAME, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        cookie.setSecure(isSecure(request));
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private static void clearCookie(HttpServletRequest request, HttpServletResponse response) {
        writeCookie(request, response, "", 0);
    }

    private static boolean isSecure(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return forwardedProto != null && forwardedProto.toLowerCase().contains("https");
    }

    private static String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                ObjectOutputStream output = new ObjectOutputStream(buffer)) {
            output.writeObject(authorizationRequest);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.toByteArray());
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not serialize the OAuth2 authorization request", ex);
        }
    }

    private static OAuth2AuthorizationRequest deserialize(String value) {
        try (ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(Base64.getUrlDecoder().decode(value)))) {
            return (OAuth2AuthorizationRequest) input.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            return null;
        }
    }
}
