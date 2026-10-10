package com.lacivita.turnos.notifications.application;

import com.lacivita.turnos.notifications.domain.DemoEmail;
import com.lacivita.turnos.notifications.domain.DemoEmailRepository;
import com.lacivita.turnos.shared.domain.Email;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Buzón del modo demo: muestra, a quien escribe la dirección, los emails que se guardaron en lugar de
 * enviarse.
 *
 * <p>DECISIÓN: el buzón de una dirección lo ve cualquiera que la escriba, sin contraseña. Es una demo: la
 * página pide usar direcciones inventadas. Así se pueden probar el código de la reserva, el link de acceso
 * y los avisos de turnos sin dejar un email real.
 */
@Service
@ConditionalOnBooleanProperty("app.demo.enabled")
public class DemoInbox {

    private final DemoEmailRepository emails;

    DemoInbox(DemoEmailRepository emails) {
        this.emails = emails;
    }

    @Transactional(readOnly = true)
    public List<DemoEmailView> inboxOf(Email address) {
        return emails.findTop20ByRecipientOrderByCreatedAtDesc(address.value()).stream()
                .map(DemoEmailView::of)
                .toList();
    }

    /** El evento de calendario de un email, para descargarlo como {@code .ics}. */
    @Transactional(readOnly = true)
    public Optional<String> calendarOf(UUID emailId, Email address) {
        return emails.findByIdAndRecipient(emailId, address.value()).flatMap(DemoEmail::calendar);
    }

    public record DemoEmailView(
            UUID id, String subject, String text, String html, boolean hasCalendar, Instant createdAt) {

        static DemoEmailView of(DemoEmail email) {
            return new DemoEmailView(
                    email.getId(),
                    email.getSubject(),
                    email.getTextBody(),
                    email.html().orElse(null),
                    email.calendar().isPresent(),
                    email.getCreatedAt());
        }
    }
}
