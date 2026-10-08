package com.lacivita.turnos.users.domain;

import com.lacivita.turnos.shared.domain.ConflictException;

/** La persona ya forma parte del equipo del negocio. */
public class AlreadyMemberException extends ConflictException {

    public AlreadyMemberException() {
        super("already_member", "Esa persona ya forma parte del equipo.");
    }
}
