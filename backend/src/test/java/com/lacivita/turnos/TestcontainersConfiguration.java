package com.lacivita.turnos;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL real para las pruebas de integración, con la misma versión y los mismos usuarios que en
 * desarrollo y producción: Flyway migra con el dueño de las tablas y la aplicación se conecta con
 * {@code turnos_app}, sin privilegios, para que Row Level Security aplique de verdad.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    static final String APP_USER = "turnos_app";
    static final String APP_PASSWORD = "turnos_app";

    @Bean
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17.11-alpine"))
                .withInitScript("testcontainers/app-role.sql");
    }

    @Bean
    DynamicPropertyRegistrar databaseProperties(PostgreSQLContainer postgres) {
        return registry -> {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", () -> APP_USER);
            registry.add("spring.datasource.password", () -> APP_PASSWORD);
            registry.add("spring.flyway.url", postgres::getJdbcUrl);
            registry.add("spring.flyway.user", postgres::getUsername);
            registry.add("spring.flyway.password", postgres::getPassword);
        };
    }
}
