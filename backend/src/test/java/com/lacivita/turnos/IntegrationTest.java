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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ActiveProfiles;

/**
 * Prueba de integración con la aplicación completa, PostgreSQL real, emails registrados en memoria y un
 * reloj que se puede adelantar.
 *
 * <p>Todas las pruebas con esta anotación comparten el mismo contexto de Spring (y el mismo contenedor),
 * así que no deben depender de una base vacía: cada una usa sus propios datos (por ejemplo, emails
 * únicos).
 *
 * <p>Las notificaciones no salen solas: la prueba llama a {@code NotificationDispatcher.dispatchDue()} y
 * las lee de {@link RecordingNotificationChannel}.
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

        /** Antes que el canal de email real: rige el primero de cada tipo. */
        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        RecordingNotificationChannel recordingNotificationChannel() {
            return new RecordingNotificationChannel();
        }

        @Bean
        @Primary
        TestClock testClock() {
            return new TestClock();
        }
    }
}
