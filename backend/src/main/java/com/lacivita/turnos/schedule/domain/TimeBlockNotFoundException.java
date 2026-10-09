package com.lacivita.turnos.schedule.domain;

import com.lacivita.turnos.shared.domain.NotFoundException;

public class TimeBlockNotFoundException extends NotFoundException {

    public TimeBlockNotFoundException() {
        super("time_block_not_found", "No encontramos ese bloqueo.");
    }
}
