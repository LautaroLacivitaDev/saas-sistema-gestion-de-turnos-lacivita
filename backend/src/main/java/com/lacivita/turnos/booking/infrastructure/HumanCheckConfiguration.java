package com.lacivita.turnos.booking.infrastructure;

import com.lacivita.turnos.booking.domain.HumanCheck;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Elige la verificación anti-bots: Turnstile si hay clave secreta; si no, una que deja pasar todo y lo
 * avisa en el log. En producción la clave es obligatoria (ver {@code application-prod.yml}).
 */
@Configuration(proxyBeanMethods = false)
class HumanCheckConfiguration {

    private static final Logger log = LoggerFactory.getLogger(HumanCheckConfiguration.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Bean
    HumanCheck humanCheck(TurnstileProperties properties) {
        if (!properties.isEnabled()) {
            log.warn("Turnstile desactivado (falta app.turnstile.secret-key): no se verifica que los invitados"
                    + " sean personas. Usar solo en desarrollo y pruebas.");
            return (token, remoteIp) -> true;
        }
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT);
        requestFactory.setReadTimeout(TIMEOUT);
        return new TurnstileHumanCheck(
                RestClient.builder().requestFactory(requestFactory).build(), properties.secretKey());
    }
}
