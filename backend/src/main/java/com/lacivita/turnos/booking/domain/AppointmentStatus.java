package com.lacivita.turnos.booking.domain;

import java.util.EnumSet;
import java.util.Set;

/** Estados de un turno. */
public enum AppointmentStatus {
    /** Horario reservado unos minutos mientras el cliente completa sus datos. Vence solo. */
    HOLD,
    /** Cargado por el equipo, a confirmar con el cliente. */
    PENDING,
    CONFIRMED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    /** Estados que ocupan el horario del profesional (la restricción de exclusión usa los mismos). */
    public static final Set<AppointmentStatus> OCCUPYING = EnumSet.of(HOLD, PENDING, CONFIRMED, IN_PROGRESS, COMPLETED);

    /** Estados que se ven en la agenda del equipo (todo menos los bloqueos temporales). */
    public static final Set<AppointmentStatus> IN_AGENDA =
            EnumSet.of(PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW);
}
