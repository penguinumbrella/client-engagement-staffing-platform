package com.skillstorm.api_gateway.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .authorizeHttpRequests(auth -> auth

                // Login/register accessible without JWT
                .requestMatchers("/api/auth/**").permitAll()

                // Health endpoint
                .requestMatchers("/actuator/health/**").permitAll()

                // Everything else requires JWT
                .anyRequest().authenticated()
            )

            .oauth2ResourceServer(oauth ->
                oauth.jwt(Customizer.withDefaults())
            );

        return http.build();
    }
}