package com.lacivita.turnos.notifications.domain;

import com.lacivita.turnos.shared.domain.Ids;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.springframework.data.domain.Persistable;

/** Aviso que una persona del equipo ve dentro de la app, por ejemplo "Nuevo turno: Corte con Ana". */
@Entity
@Table(name = "app_notice")
public class AppNotice implements Persistable<UUID> {

    static final int MAX_MESSAGE = 300;

    @Id
    private UUID id;

    @TenantId
    private UUID businessId;

    private UUID userId;

    private UUID appointmentId;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String message;

    private Instant createdAt;

    private Instant readAt;

    /** Sin columna de versión: así Spring Data distingue un alta (el id lo asignamos nosotros). */
    @Transient
    private boolean isNew = true;

    protected AppNotice() {
        // Requerido por JPA.
    }

    private AppNotice(
            UUID businessId, UUID userId, UUID appointmentId, NotificationType type, String message, Instant now) {
        if (type == NotificationType.APPOINTMENT_REMINDER || type == NotificationType.DAILY_AGENDA) {
            throw new IllegalArgumentException("Los avisos en la app son de cambios en los turnos");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("El aviso necesita un texto");
        }
        this.id = Ids.newId();
        this.businessId = Objects.requireNonNull(businessId, "businessId");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.appointmentId = Objects.requireNonNull(appointmentId, "appointmentId");
        this.type = type;
        this.message = message.length() <= MAX_MESSAGE ? message : message.substring(0, MAX_MESSAGE - 1) + "…";
        this.createdAt = now;
    }

    public static AppNotice of(
            UUID businessId, UUID userId, UUID appointmentId, NotificationType type, String message, Instant now) {
        return new AppNotice(businessId, userId, appointmentId, type, message, now);
    }

    /** Marcar como leído dos veces no cambia la fecha de la primera lectura. */
    public void markRead(Instant now) {
        if (readAt == null) {
            this.readAt = now;
        }
    }

    public boolean belongsTo(UUID user) {
        return userId.equals(user);
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

    public UUID getAppointmentId() {
        return appointmentId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Optional<Instant> readAt() {
        return Optional.ofNullable(readAt);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof AppNotice notice && id != null && id.equals(notice.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
