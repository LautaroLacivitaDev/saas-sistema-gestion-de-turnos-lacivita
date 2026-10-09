package com.lacivita.turnos.booking.domain;

import com.lacivita.turnos.shared.domain.ConflictException;

/** El horario ya lo tomó otra persona, o ya no se ofrece. */
public class SlotNotAvailableException extends ConflictException {

    public SlotNotAvailableException() {
        super("slot_not_available", "Ese horario ya no está disponible. Elegí otro.");
    }
}
