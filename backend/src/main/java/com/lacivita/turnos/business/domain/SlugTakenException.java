package com.lacivita.turnos.business.domain;

import com.lacivita.turnos.shared.domain.ConflictException;

/** El slug lo usa o lo usó otro negocio (los slugs usados quedan reservados para redirigir). */
public class SlugTakenException extends ConflictException {

    public SlugTakenException() {
        super("slug_taken", "Ese link ya lo usa otro negocio. Probá con otro.");
    }
}
