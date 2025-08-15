package com.elamal.stockmanagement.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ✅ SOLUTION 1: Utiliser allowedOriginPatterns au lieu de allowedOrigins
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));

        // OU

        // ✅ SOLUTION 2: Spécifier des origines explicites (RECOMMANDÉ pour la production)
        // configuration.setAllowedOrigins(Arrays.asList(
        //     "http://localhost:4200",     // Angular dev
        //     "http://localhost:3000",     // React dev
        //     "http://localhost:8081",     // Frontend alternatif
        //     "https://votre-domaine.com"  // Production
        // ));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        configuration.setExposedHeaders(Arrays.asList(
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

