package com.lacivita.turnos.catalog.domain;

import com.lacivita.turnos.shared.domain.NotFoundException;

public class ComboNotFoundException extends NotFoundException {

    public ComboNotFoundException() {
        super("combo_not_found", "No encontramos ese combo.");
    }
}
