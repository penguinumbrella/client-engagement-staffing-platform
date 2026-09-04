package com.skillstorm.auth_service.Config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2LoginFailureHandlerTest {

    @Test
    void redirectsToFrontendLoginInsteadOfBackendIp() throws Exception {
        OAuth2LoginFailureHandler handler = new OAuth2LoginFailureHandler(
                "https://d1r0oi9vzejxs3.cloudfront.net/auth/callback");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(
                new MockHttpServletRequest(),
                response,
                new BadCredentialsException("authorization_request_not_found"));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://d1r0oi9vzejxs3.cloudfront.net/login?error=oauth");
    }
}
