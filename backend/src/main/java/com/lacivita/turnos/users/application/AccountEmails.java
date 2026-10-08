package com.lacivita.turnos.users.application;

import com.lacivita.turnos.shared.config.AppProperties;
import com.lacivita.turnos.shared.mail.MailMessage;
import com.lacivita.turnos.shared.mail.Mailer;
import com.lacivita.turnos.users.domain.TokenSecret;
import com.lacivita.turnos.users.domain.User;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

/**
 * Emails de la cuenta. Los links apuntan a páginas del frontend, que reciben el token y llaman a la
 * API.
 */
// DECISIÓN: emails de texto plano por ahora. Las plantillas HTML editables llegan con el módulo de
// notificaciones (Hito 7).
@Component
class AccountEmails {

    static final String VERIFY_EMAIL_PATH = "/verificar-email?token=";
    static final String LOGIN_LINK_PATH = "/acceso?token=";

    private final Mailer mailer;
    private final AppProperties properties;

    AccountEmails(Mailer mailer, AppProperties properties) {
        this.mailer = mailer;
        this.properties = properties;
    }

    void sendEmailVerification(User user, TokenSecret secret) {
        String link = properties.frontendLink(VERIFY_EMAIL_PATH + encode(secret));
        mailer.sendAfterCommit(
                new MailMessage(user.getEmail(), "Confirmá tu email", """
                Hola %s:

                Para confirmar tu email, entrá a este link:
                %s

                El link vence en 24 horas. Si no creaste una cuenta, ignorá este mensaje.
                """.formatted(user.getName(), link)));
    }

    void sendLoginLink(User user, TokenSecret secret) {
        String link = properties.frontendLink(LOGIN_LINK_PATH + encode(secret));
        mailer.sendAfterCommit(
                new MailMessage(user.getEmail(), "Tu link para ingresar", """
                Hola %s:

                Entrá a este link para iniciar sesión:
                %s

                El link vence en 15 minutos y sirve una sola vez. Si no lo pediste, ignorá este mensaje.
                """.formatted(user.getName(), link)));
    }

    private static String encode(TokenSecret secret) {
        return URLEncoder.encode(secret.value(), StandardCharsets.UTF_8);
    }
}
