package com.skillstorm.auth_service.Config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import static org.assertj.core.api.Assertions.assertThat;

class CookieOAuth2AuthorizationRequestRepositoryTest {

    @Test
    void roundTripsAuthorizationRequestThroughCookie() {
        CookieOAuth2AuthorizationRequestRepository repository =
                new CookieOAuth2AuthorizationRequestRepository();

        OAuth2AuthorizationRequest original = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("client")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .redirectUri("https://du83k7mttey3e.cloudfront.net/auth/login/oauth2/code/google")
                .state("state-value")
                .build();

        MockHttpServletRequest saveRequest = new MockHttpServletRequest();
        saveRequest.addHeader("X-Forwarded-Proto", "https");
        MockHttpServletResponse saveResponse = new MockHttpServletResponse();
        repository.saveAuthorizationRequest(original, saveRequest, saveResponse);

        MockHttpServletRequest loadRequest = new MockHttpServletRequest();
        loadRequest.setCookies(saveResponse.getCookies());
        MockHttpServletResponse loadResponse = new MockHttpServletResponse();

        OAuth2AuthorizationRequest loaded =
                repository.removeAuthorizationRequest(loadRequest, loadResponse);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getClientId()).isEqualTo("client");
        assertThat(loaded.getState()).isEqualTo("state-value");
        assertThat(loaded.getRedirectUri())
                .isEqualTo("https://du83k7mttey3e.cloudfront.net/auth/login/oauth2/code/google");
    }
}
