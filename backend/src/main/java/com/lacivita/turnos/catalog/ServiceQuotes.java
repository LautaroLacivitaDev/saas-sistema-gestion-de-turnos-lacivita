package com.lacivita.turnos.catalog;

import java.util.Optional;
import java.util.UUID;

/** Cotiza lo que se puede reservar con un profesional. Lo usan la agenda y las reservas. */
public interface ServiceQuotes {

    /**
     * Precio y duración del servicio o combo con ese profesional. Vacío si no se puede reservar: no existe,
     * está retirado o el profesional no hace alguno de sus servicios.
     */
    Optional<Quote> quote(UUID businessId, UUID barberId, BookableItem item);
}
