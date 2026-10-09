package com.lacivita.turnos.notifications.domain;

import com.lacivita.turnos.shared.domain.Ids;
import com.lacivita.turnos.shared.domain.InvalidValueException;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

/** Texto propio del negocio para un tipo de email al cliente. Sin uno, rige el texto de Laciturnos. */
@Entity
@Table(name = "message_template")
public class MessageTemplate {

    private static final Map<NotificationType, TemplateText> DEFAULTS = defaults();

    @Id
    private UUID id;

    @TenantId
    private UUID businessId;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String subject;

    private String body;

    private Instant updatedAt;

    @Version
    private Long version;

    protected MessageTemplate() {
        // Requerido por JPA.
    }

    private MessageTemplate(UUID businessId, NotificationType type, TemplateText text, Instant now) {
        requireCustomizable(type);
        this.id = Ids.newId();
        this.businessId = Objects.requireNonNull(businessId, "businessId");
        this.type = type;
        change(text, now);
    }

    public static MessageTemplate custom(UUID businessId, NotificationType type, TemplateText text, Instant now) {
        return new MessageTemplate(businessId, type, text, now);
    }

    /** El texto de Laciturnos para ese tipo de email. */
    public static TemplateText defaultFor(NotificationType type) {
        requireCustomizable(type);
        return DEFAULTS.get(type);
    }

    public void change(TemplateText text, Instant now) {
        this.subject = text.subject();
        this.body = text.body();
        this.updatedAt = now;
    }

    public NotificationType getType() {
        return type;
    }

    public TemplateText text() {
        return new TemplateText(subject, body);
    }

    private static void requireCustomizable(NotificationType type) {
        if (type == null || !type.isCustomizable()) {
            throw new InvalidValueException(
                    "template_not_customizable", "Ese aviso no tiene un texto que se pueda personalizar.");
        }
    }

    private static Map<NotificationType, TemplateText> defaults() {
        var texts = new EnumMap<NotificationType, TemplateText>(NotificationType.class);
        texts.put(NotificationType.APPOINTMENT_BOOKED, new TemplateText("Tu turno en {negocio}", """
                Hola {nombre}:

                Te esperamos el {hora} en {sucursal} ({direccion}) para {servicio} con {barbero}."""));
        texts.put(NotificationType.APPOINTMENT_REMINDER, new TemplateText("Recordatorio: tu turno en {negocio}", """
                Hola {nombre}:

                Te recordamos tu turno del {hora} en {sucursal} ({direccion}): {servicio} con {barbero}.

                Si no podés venir, avisanos con el botón de abajo así le damos el horario a otra persona."""));
        texts.put(NotificationType.APPOINTMENT_RESCHEDULED, new TemplateText("Cambió tu turno en {negocio}", """
                Hola {nombre}:

                Tu turno cambió. Ahora es el {hora} en {sucursal} ({direccion}): {servicio} con {barbero}."""));
        texts.put(NotificationType.APPOINTMENT_CANCELLED, new TemplateText("Se canceló tu turno en {negocio}", """
                Hola {nombre}:

                Tu turno del {hora} en {sucursal} ({servicio} con {barbero}) quedó cancelado.

                Cuando quieras, podés reservar otro."""));
        return Map.copyOf(texts);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof MessageTemplate template && id != null && id.equals(template.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
