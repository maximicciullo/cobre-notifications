package com.cobre.notifications.integration;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresIntegrationTest.ContainersConfig.class)
public abstract class PostgresIntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class ContainersConfig {

        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgresContainer() {
            return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
        }
    }
}
