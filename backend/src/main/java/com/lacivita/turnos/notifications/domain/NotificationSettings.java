package com.lacivita.turnos.notifications.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Cómo avisa el negocio a sus clientes. Por ahora, cuándo les recuerda el turno. */
@Entity
@Table(name = "notification_settings")
public class NotificationSettings {

    @Id
    private UUID businessId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<Integer> reminderHours = new ArrayList<>();

    private Instant updatedAt;

    /** Nulo hasta el primer guardado: así Spring Data distingue un alta (el id lo asignamos nosotros). */
    @Version
    private Long version;

    protected NotificationSettings() {
        // Requerido por JPA.
    }

    private NotificationSettings(UUID businessId, ReminderSchedule reminders, Instant now) {
        this.businessId = Objects.requireNonNull(businessId, "businessId");
        change(reminders, now);
    }

    public static NotificationSettings of(UUID businessId, ReminderSchedule reminders, Instant now) {
        return new NotificationSettings(businessId, reminders, now);
    }

    public void change(ReminderSchedule reminders, Instant now) {
        this.reminderHours = new ArrayList<>(reminders.hoursBefore());
        this.updatedAt = now;
    }

    public ReminderSchedule reminders() {
        return new ReminderSchedule(List.copyOf(reminderHours));
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (other instanceof NotificationSettings settings
                        && businessId != null
                        && businessId.equals(settings.businessId));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
