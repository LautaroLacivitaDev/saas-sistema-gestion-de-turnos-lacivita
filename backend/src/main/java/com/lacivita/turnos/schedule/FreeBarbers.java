package com.lacivita.turnos.schedule;

import com.lacivita.turnos.catalog.BookableItem;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Quién puede empezar un servicio o combo a una hora dada. Lo usan las reservas antes de tomar un horario. */
public interface FreeBarbers {

    /**
     * Profesionales de la sucursal que pueden empezar a esa hora, con las mismas reglas que la
     * disponibilidad pública (horarios, feriados, bloqueos, turnos, preparación y anticipación).
     *
     * @param barberId un profesional en particular; {@code null} para cualquiera
     * @param ignoringBooking un turno que no cuenta como ocupado (el que se está reprogramando); puede ser
     *     {@code null}
     */
    List<UUID> at(
            UUID businessId, UUID branchId, BookableItem item, UUID barberId, Instant start, UUID ignoringBooking);
}
