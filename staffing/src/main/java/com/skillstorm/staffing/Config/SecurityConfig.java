package com.skillstorm.staffing.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .authorizeHttpRequests(auth -> auth

                // Health/ERROR endpoint
                .requestMatchers("/actuator/health/**","/error")
                    .permitAll()

                /*
                 * CONSULTANTS
                 *
                 * All authenticated users can view the consultant roster
                 * and individual consultant records.
                 */
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/consultants",
                    "/api/consultants/**"
                )
                    .authenticated()

                /*
                 * Only Engagement Managers can create consultants.
                 */
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/consultants",
                    "/api/consultants/**"
                )
                    .hasRole("ENGAGEMENT_MANAGER")

                /*
                 * Only Engagement Managers can update consultants.
                 */
                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/consultants/**"
                )
                    .hasRole("ENGAGEMENT_MANAGER")

                /*
                 * Only Engagement Managers can delete consultants.
                 */
                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/consultants/**"
                )
                    .hasRole("ENGAGEMENT_MANAGER")

                /*
                 * ASSIGNMENTS
                 *
                 * Only Engagement Managers can currently look up arbitrary
                 * consultant or engagement assignments.
                 */
                .requestMatchers(
                    "/api/assignments/me/**"
                ).authenticated()
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/assignments/consultant/**",
                    "/api/assignments/engagement/**"
                )
                    .hasRole("ENGAGEMENT_MANAGER")

                /*
                 * Only Engagement Managers can create assignments.
                 */
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/assignments",
                    "/api/assignments/**"
                )
                    .hasRole("ENGAGEMENT_MANAGER")

                /*
                 * Only Engagement Managers can remove assignments.
                 */
                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/assignments/**"
                )
                    .hasRole("ENGAGEMENT_MANAGER")

                // Anything we forgot to explicitly allow is blocked.
                .anyRequest()
                    .denyAll()
            )

            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt ->
                    jwt.jwtAuthenticationConverter(
                        jwtAuthenticationConverter()
                    )
                )
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter authoritiesConverter =
            new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter =
            new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
            authoritiesConverter
        );

        return converter;
    }
}