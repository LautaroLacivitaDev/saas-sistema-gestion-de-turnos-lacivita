package com.lacivita.turnos.booking;

import com.lacivita.turnos.shared.audit.AuditableEvent;
import java.util.Optional;
import java.util.UUID;

/**
 * Cambio en un turno de un negocio. Se publica dentro de la transacción del cambio: quien escucha (la
 * auditoría, las notificaciones) escribe en la misma transacción y con el mismo negocio.
 */
public sealed interface AppointmentEvent extends AuditableEvent
        permits AppointmentBooked, AppointmentStatusChanged, AppointmentRescheduled {

    UUID businessId();

    UUID appointmentId();

    @Override
    default Optional<UUID> auditBusinessId() {
        return Optional.of(businessId());
    }

    @Override
    default String auditEntityType() {
        return "Appointment";
    }

    @Override
    default String auditEntityId() {
        return appointmentId().toString();
    }
}
