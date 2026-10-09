package com.lacivita.turnos.booking;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Se reservó un turno: online (al confirmarlo) o desde el local.
 *
 * @param source {@code WEB} o {@code COUNTER}
 * @param status {@code CONFIRMED} o {@code PENDING} (a confirmar con el cliente)
 */
public record AppointmentBooked(
        UUID businessId,
        UUID appointmentId,
        UUID branchId,
        UUID barberId,
        Instant startsAt,
        String source,
        String status)
        implements AppointmentEvent {

    @Override
    public String auditAction() {
        return "appointment.booked";
    }

    @Override
    public Optional<Object> auditAfter() {
        return Optional.of(
                Map.of("barberId", barberId, "startsAt", startsAt.toString(), "source", source, "status", status));
    }
}
