package com.lacivita.turnos.booking;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Un turno se movió de horario, de profesional o de ambos. Conserva los servicios y el precio. */
public record AppointmentRescheduled(
        UUID businessId,
        UUID appointmentId,
        UUID branchId,
        UUID barberBefore,
        Instant startBefore,
        UUID barberAfter,
        Instant startAfter)
        implements AppointmentEvent {

    public boolean changedBarber() {
        return !barberBefore.equals(barberAfter);
    }

    @Override
    public String auditAction() {
        return "appointment.rescheduled";
    }

    @Override
    public Optional<Object> auditBefore() {
        return Optional.of(slot(barberBefore, startBefore));
    }

    @Override
    public Optional<Object> auditAfter() {
        return Optional.of(slot(barberAfter, startAfter));
    }

    private static Map<String, Object> slot(UUID barberId, Instant startsAt) {
        var map = new HashMap<String, Object>();
        map.put("barberId", barberId);
        map.put("startsAt", startsAt.toString());
        return map;
    }
}
