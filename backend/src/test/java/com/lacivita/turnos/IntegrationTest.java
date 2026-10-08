package com.lacivita.turnos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

/**
 * Prueba de integración con la aplicación completa, PostgreSQL real y emails registrados en memoria.
 *
 * <p>Todas las pruebas con esta anotación comparten el mismo contexto de Spring (y el mismo contenedor),
 * así que no deben depender de una base vacía: cada una usa sus propios datos (por ejemplo, emails
 * únicos).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, IntegrationTest.MailConfiguration.class})
public @interface IntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    class MailConfiguration {

        @Bean
        @Primary
        RecordingMailer recordingMailer() {
            return new RecordingMailer();
        }
    }
}
