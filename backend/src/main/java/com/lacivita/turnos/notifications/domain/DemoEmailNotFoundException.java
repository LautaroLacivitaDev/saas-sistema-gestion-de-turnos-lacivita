package com.lacivita.turnos.notifications.domain;

import com.lacivita.turnos.shared.domain.NotFoundException;

/** El email del buzón de la demo no existe o no es de esa dirección. */
public class DemoEmailNotFoundException extends NotFoundException {

    public DemoEmailNotFoundException() {
        super("demo_email_not_found", "No encontramos ese email.");
    }
}
