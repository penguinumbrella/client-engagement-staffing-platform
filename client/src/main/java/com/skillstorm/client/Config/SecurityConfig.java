package com.skillstorm.client.Config;

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

                .requestMatchers("/actuator/health/**")
                    .permitAll()

                // Both roles can view clients
                .requestMatchers(
                    HttpMethod.GET,
                    "/clients",
                    "/clients/**"
                )
                    .authenticated()

                // Only Engagement Managers can create clients
                .requestMatchers(
                    HttpMethod.POST,
                    "/clients",
                    "/clients/**"
                )
                    .hasRole("ENGAGEMENT_MANAGER")

                // Only Engagement Managers can edit clients
                .requestMatchers(
                    HttpMethod.PUT,
                    "/clients/**"
                )
                    .hasRole("ENGAGEMENT_MANAGER")

                .requestMatchers(
                    HttpMethod.PATCH,
                    "/clients/**"
                )
                    .hasRole("ENGAGEMENT_MANAGER")

                // Only Engagement Managers can delete clients
                .requestMatchers(
                    HttpMethod.DELETE,
                    "/clients/**"
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