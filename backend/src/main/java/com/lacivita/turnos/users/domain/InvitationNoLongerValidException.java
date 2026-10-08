package com.lacivita.turnos.users.domain;

import com.lacivita.turnos.shared.domain.RuleViolationException;

/** La invitación ya se aceptó, se revocó o venció. No se distingue cuál, igual que con los tokens. */
public class InvitationNoLongerValidException extends RuleViolationException {

    public InvitationNoLongerValidException() {
        super("invitation_no_longer_valid", "La invitación ya no es válida. Pedí una nueva.");
    }
}
