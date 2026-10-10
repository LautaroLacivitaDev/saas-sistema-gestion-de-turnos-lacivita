package com.lacivita.turnos.notifications.domain;

import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.domain.Ids;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

/**
 * Email guardado en el buzón del modo demo en lugar de enviarse. Quien prueba la app lo ve en la página del
 * buzón, con sus links y el {@code .ics}, sin dejar su dirección real.
 */
@Entity
@Table(name = "demo_email")
public class DemoEmail implements Persistable<UUID> {

    static final int MAX_SUBJECT = 200;

    @Id
    private UUID id;

    private String recipient;

    private String subject;

    private String textBody;

    private String htmlBody;

    private String calendar;

    private Instant createdAt;

    /** Sin columna de versión: así Spring Data distingue un alta (el id lo asignamos nosotros). */
    @Transient
    private boolean isNew = true;

    protected DemoEmail() {
        // Requerido por JPA.
    }

    private DemoEmail(Email to, String subject, String text, String html, String calendar, Instant now) {
        if (subject == null || subject.isBlank() || text == null || text.isBlank()) {
            throw new IllegalArgumentException("El email necesita asunto y texto");
        }
        this.id = Ids.newId();
        this.recipient = Objects.requireNonNull(to, "to").value();
        this.subject = subject.length() <= MAX_SUBJECT ? subject : subject.substring(0, MAX_SUBJECT - 1) + "…";
        this.textBody = text;
        this.htmlBody = html;
        this.calendar = calendar;
        this.createdAt = Objects.requireNonNull(now, "now");
    }

    /** Un email de texto, como los de la cuenta. */
    public static DemoEmail text(Email to, String subject, String text, Instant now) {
        return new DemoEmail(to, subject, text, null, null, now);
    }

    /** Un aviso de turno: HTML, su versión en texto y, si lo tiene, el evento de calendario. */
    public static DemoEmail rich(Email to, String subject, String text, String html, String calendar, Instant now) {
        return new DemoEmail(to, subject, text, Objects.requireNonNull(html, "html"), calendar, now);
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markStored() {
        this.isNew = false;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getTextBody() {
        return textBody;
    }

    public Optional<String> html() {
        return Optional.ofNullable(htmlBody);
    }

    public Optional<String> calendar() {
        return Optional.ofNullable(calendar);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof DemoEmail email && id != null && id.equals(email.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
