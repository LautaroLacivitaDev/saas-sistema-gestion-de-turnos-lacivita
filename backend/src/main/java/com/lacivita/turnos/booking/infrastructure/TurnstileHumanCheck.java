package com.lacivita.turnos.booking.infrastructure;

import com.lacivita.turnos.booking.domain.HumanCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Verificación anti-bots con Cloudflare Turnstile: el navegador resuelve el desafío y el servidor valida
 * el token con Cloudflare.
 *
 * <p>Si Cloudflare no responde, se rechaza: es preferible que un invitado reintente a dejar pasar bots que
 * llenen la agenda o envíen emails masivos.
 */
class TurnstileHumanCheck implements HumanCheck {

    static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private static final Logger log = LoggerFactory.getLogger(TurnstileHumanCheck.class);

    private final RestClient client;
    private final String secretKey;

    TurnstileHumanCheck(RestClient client, String secretKey) {
        this.client = client;
        this.secretKey = secretKey;
    }

    @Override
    public boolean isHuman(String token, String remoteIp) {
        if (token == null || token.isBlank()) {
            return false;
        }
        var form = new LinkedMultiValueMap<String, String>();
        form.add("secret", secretKey);
        form.add("response", token);
        if (remoteIp != null) {
            form.add("remoteip", remoteIp);
        }
        try {
            var result = client.post()
                    .uri(VERIFY_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(SiteVerifyResponse.class);
            return result != null && result.success();
        } catch (RestClientException ex) {
            log.warn("No se pudo validar el token de Turnstile: {}", ex.getMessage());
            return false;
        }
    }

    /** Lo que responde Cloudflare. Solo interesa si el token es válido. */
    record SiteVerifyResponse(boolean success) {}
}
