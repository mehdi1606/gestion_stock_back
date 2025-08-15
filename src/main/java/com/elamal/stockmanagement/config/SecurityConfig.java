package com.elamal.stockmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for API
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/**").permitAll() // Allow all API endpoints
                        .requestMatchers("/swagger-ui/**").permitAll() // Allow Swagger UI
                        .requestMatchers("/swagger-ui.html").permitAll() // Allow Swagger UI
                        .requestMatchers("/api-docs/**").permitAll() // Allow API docs
                        .requestMatchers("/v3/api-docs/**").permitAll() // Allow OpenAPI docs
                        .requestMatchers("/h2-console/**").permitAll() // Allow H2 console if used
                        .anyRequest().authenticated() // Require auth for other endpoints
                )
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.disable()) // Updated API for frame options
                        .contentTypeOptions(contentTypeOptions -> contentTypeOptions.disable())
                        .referrerPolicy(referrerPolicy -> referrerPolicy.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)) // Use enum
                );

        return http.build();
    }
}
