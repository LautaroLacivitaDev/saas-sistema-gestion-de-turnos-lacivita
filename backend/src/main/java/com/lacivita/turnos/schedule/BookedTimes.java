package com.lacivita.turnos.schedule;

import com.lacivita.turnos.shared.domain.TimeInterval;
import java.util.List;
import java.util.UUID;

/**
 * Punto de extensión: los turnos ya tomados de un profesional, que la disponibilidad descuenta. Lo
 * implementa el módulo de reservas; la agenda no conoce los turnos y así no depende de ese módulo.
 */
public interface BookedTimes {

    /** Turnos del profesional que se cruzan con el período. */
    List<TimeInterval> of(UUID businessId, UUID barberId, TimeInterval period);
}
