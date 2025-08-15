package com.elamal.stockmanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Stock Management API")
                        .description("API complète de gestion de stock avec dashboard et analytics")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ELAMAL Stock Management")
                                .email("contact@elamal.com")
                                .url("https://elamal.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8090")
                                .description("Environnement de développement"),
                        new Server()
                                .url("https://api.stockmanagement.elamal.com")
                                .description("Environnement de production")
                ));
    }
}
