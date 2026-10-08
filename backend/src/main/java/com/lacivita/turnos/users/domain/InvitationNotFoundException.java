package com.lacivita.turnos.users.domain;

import com.lacivita.turnos.shared.domain.NotFoundException;

public class InvitationNotFoundException extends NotFoundException {

    public InvitationNotFoundException() {
        super("invitation_not_found", "No encontramos esa invitación.");
    }
}
