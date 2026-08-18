package com.example.nova.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI is served at /swagger-ui.html, the raw spec at /v3/api-docs.
 * Both paths are permitted without authentication (see SecurityConfig) so the
 * docs themselves are browsable; each individual operation still requires the
 * same JWT the live API enforces - use the "Authorize" button with a token
 * obtained from POST /api/auth/login to try protected endpoints from the UI.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI secureAppOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Nova App API")
                        .version("1.0.0")
                        .description("Spring Boot + Spring Security 6 reference API: JWT auth, MFA, SSO, "
                                + "and the AI companion/project/meeting domain built on top of it.")
                        .contact(new Contact().name("Nova App")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
