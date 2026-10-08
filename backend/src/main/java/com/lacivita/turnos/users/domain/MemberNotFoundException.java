package com.lacivita.turnos.users.domain;

import com.lacivita.turnos.shared.domain.NotFoundException;

public class MemberNotFoundException extends NotFoundException {

    public MemberNotFoundException() {
        super("member_not_found", "Esa persona no forma parte del equipo.");
    }
}
