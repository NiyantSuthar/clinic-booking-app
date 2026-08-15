package com.clinic.booking.config;

import com.clinic.booking.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * - /auth/**            -> public
 * - /bookings/status     -> public (a capacity check, no personal data - deliberately carved out BEFORE the /bookings/** rule below)
 * - /bookings/**         -> ROLE_PATIENT (new this session - was fully open before)
 * - /beneficiaries/**    -> ROLE_PATIENT
 * - /account/**          -> ROLE_PATIENT
 * - /admin/**            -> ROLE_ADMIN
 *
 * Order matters here: Spring Security evaluates requestMatchers top to
 * bottom and uses the FIRST match, so the more specific /bookings/status
 * rule must be listed before the broader /bookings/** rule, or the
 * broader rule would win first and status would incorrectly require auth too.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/bookings/status").permitAll()
                        .requestMatchers("/bookings/**").hasRole("PATIENT")
                        .requestMatchers("/beneficiaries/**").hasRole("PATIENT")
                        .requestMatchers("/account/**").hasRole("PATIENT")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}