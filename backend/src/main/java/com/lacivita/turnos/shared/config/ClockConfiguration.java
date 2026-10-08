package com.lacivita.turnos.shared.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj de la aplicación, siempre en UTC. Se inyecta en lugar de llamar a {@code Instant.now()} para
 * que las reglas que dependen del tiempo (vencimientos, anticipación) se puedan probar.
 */
@Configuration(proxyBeanMethods = false)
class ClockConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
