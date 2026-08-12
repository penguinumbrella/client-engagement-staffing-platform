package com.skillstorm.engagement.Config;

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

                // Health check
                .requestMatchers("/actuator/health/**")
                    .permitAll()

                // Engagement Managers can view all engagements and consultants can view theirs
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/engagements",
                        "/api/engagements/**"
                )
                .hasAnyRole(
                        "CONSULTANT",
                        "ENGAGEMENT_MANAGER"
                )

                // Only Engagement Managers can create engagements
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/engagements",
                    "/api/engagements/**"
                )
                    .hasRole("ENGAGEMENT_MANAGER")

                // Only Engagement Managers can update engagements
                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/engagements/**"
                )
                    .hasRole("ENGAGEMENT_MANAGER")

                // Only Engagement Managers can delete engagements
                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/engagements/**"
                )
                    .hasRole("ENGAGEMENT_MANAGER")

                .anyRequest().denyAll()
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