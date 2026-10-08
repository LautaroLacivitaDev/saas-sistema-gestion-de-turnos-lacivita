package com.lacivita.turnos.users.domain;

import com.lacivita.turnos.shared.domain.ConflictException;

public class EmailAlreadyRegisteredException extends ConflictException {

    public EmailAlreadyRegisteredException() {
        super(
                "email_already_registered",
                "Ya existe una cuenta con ese email. Iniciá sesión o pedí un link de acceso.");
    }
}
