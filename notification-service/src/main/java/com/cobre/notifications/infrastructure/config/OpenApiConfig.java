package com.cobre.notifications.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String API_KEY_SCHEME = "ApiKeyAuth";

    @Bean
    public OpenAPI notificationServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cobre Notification Service — Self-Service API")
                        .version("v1")
                        .description("""
                                Task 2 of the Cobre notifications challenge: query notification events,
                                inspect a single event's delivery status, and replay a definitively-failed
                                delivery. See DESIGN.md and SECURITY.md in the repository root for the
                                full design rationale and the OWASP mitigations implemented here.
                                """)
                        .contact(new Contact().name("Cobre Notification Service")))
                .addSecurityItem(new SecurityRequirement().addList(API_KEY_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(API_KEY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Api-Key")
                                .description("""
                                        Per-client API key. Resolves server-side to exactly one client_id —
                                        see ApiKeyAuthFilter and SECURITY.md (A01). Demo keys are seeded in
                                        V2__seed_subscriptions.sql: demo-api-key-client001/002/003.
                                        """)));
    }
}
