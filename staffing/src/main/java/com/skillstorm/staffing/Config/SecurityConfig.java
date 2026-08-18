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
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .authorizeHttpRequests(auth -> auth

                /*
                 * PUBLIC
                 */
                .requestMatchers(
                    "/actuator/health/**",
                    "/error"
                )
                    .permitAll()


                /*
                 * CONSULTANTS
                 *
                 * Any authenticated user can view consultants.
                 */
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/consultants",
                    "/api/consultants/**"
                )
                    .authenticated()


                /*
                 * A newly registered consultant can provision
                 * their own consultant profile.
                 */
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/consultants/provision"
                )
                    .hasRole("CONSULTANT")


                /*
                 * Only Engagement Managers can manually
                 * create consultant records.
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
                 * ASSIGNMENTS - CURRENT USER
                 *
                 * Consultants need access to their own assignments,
                 * engagement IDs, and team lookup endpoints.
                 */
                .requestMatchers(
                    "/api/assignments/me",
                    "/api/assignments/me/**"
                )
                    .authenticated()


                /*
                 * ASSIGNMENTS - MANAGER LOOKUPS
                 *
                 * Arbitrary consultant/engagement assignment
                 * lookup is manager-only.
                 */
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/assignments/consultant/**",
                    "/api/assignments/engagement/**"
                )
                    .hasRole("ENGAGEMENT_MANAGER")


                /*
                 * ASSIGNMENTS - STATUS UPDATES
                 *
                 * Includes:
                 *
                 * PATCH /api/assignments/{id}/status
                 *
                 * PATCH
                 * /api/assignments/engagement/{engagementId}/cascade-status
                 */
                .requestMatchers(
                    HttpMethod.PATCH,
                    "/api/assignments/*/status",
                    "/api/assignments/engagement/*/cascade-status"
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
                 * Only Engagement Managers can remove assignments
                 * or cascade engagement removal.
                 */
                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/assignments/**"
                )
                    .hasRole("ENGAGEMENT_MANAGER")


                /*
                 * Block anything we did not explicitly allow.
                 */
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

        /*
         * JWT:
         *
         * "roles": ["CONSULTANT"]
         *
         * becomes:
         *
         * ROLE_CONSULTANT
         */
        authoritiesConverter.setAuthoritiesClaimName(
            "roles"
        );

        authoritiesConverter.setAuthorityPrefix(
            "ROLE_"
        );


        JwtAuthenticationConverter converter =
            new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
            authoritiesConverter
        );

        return converter;
    }
}