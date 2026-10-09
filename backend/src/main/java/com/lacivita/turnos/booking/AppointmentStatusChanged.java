package com.lacivita.turnos.booking;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Un turno cambió de estado (por ejemplo, se confirmó, se canceló o el cliente no vino).
 *
 * @param before estado anterior, por ejemplo {@code CONFIRMED}
 * @param after estado nuevo, por ejemplo {@code CANCELLED}
 */
public record AppointmentStatusChanged(UUID businessId, UUID appointmentId, String before, String after)
        implements AppointmentEvent {

    /** Estados en los que el turno todavía va a ocurrir. */
    private static final Set<String> UPCOMING = Set.of("PENDING", "CONFIRMED");

    public boolean isCancellation() {
        return "CANCELLED".equals(after);
    }

    /** {@code true} si el turno ya no va a ocurrir o ya empezó: los recordatorios dejan de tener sentido. */
    public boolean leftTheUpcomingAgenda() {
        return !UPCOMING.contains(after);
    }

    @Override
    public String auditAction() {
        return "appointment.status_changed";
    }

    @Override
    public Optional<Object> auditBefore() {
        return Optional.of(Map.of("status", before));
    }

    @Override
    public Optional<Object> auditAfter() {
        return Optional.of(Map.of("status", after));
    }
}
