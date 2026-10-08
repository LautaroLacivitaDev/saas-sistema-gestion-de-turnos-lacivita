package com.lacivita.turnos.users.application;

import com.lacivita.turnos.shared.config.AppProperties;
import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.mail.MailMessage;
import com.lacivita.turnos.shared.mail.Mailer;
import com.lacivita.turnos.shared.security.BusinessRole;
import com.lacivita.turnos.users.domain.TokenSecret;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

/** Email de invitación al equipo. El link lleva a una página del frontend que llama a la API. */
@Component
class TeamEmails {

    static final String INVITATION_PATH = "/invitacion?token=";

    private final Mailer mailer;
    private final AppProperties properties;

    TeamEmails(Mailer mailer, AppProperties properties) {
        this.mailer = mailer;
        this.properties = properties;
    }

    void sendInvitation(Email to, String businessName, BusinessRole role, TokenSecret secret) {
        String link =
                properties.frontendLink(INVITATION_PATH + URLEncoder.encode(secret.value(), StandardCharsets.UTF_8));
        mailer.sendAfterCommit(new MailMessage(
                to, "Te invitaron a " + businessName, """
                Hola:

                Te invitaron a sumarte a %s como %s.

                Para aceptar, entrá a este link e iniciá sesión (o creá tu cuenta) con este mismo email:
                %s

                La invitación vence en 7 días. Si no la esperabas, ignorá este mensaje.
                """.formatted(businessName, roleName(role), link)));
    }

    private static String roleName(BusinessRole role) {
        return switch (role) {
            case OWNER -> "dueño";
            case MANAGER -> "gerente";
            case BARBER -> "profesional";
        };
    }
}
