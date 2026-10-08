package com.lacivita.turnos.users.domain;

import com.lacivita.turnos.shared.domain.RuleViolationException;

/** La invitación es para otro email: hay que entrar con la cuenta del email invitado. */
public class InvitationEmailMismatchException extends RuleViolationException {

    public InvitationEmailMismatchException() {
        super(
                "invitation_email_mismatch",
                "Esta invitación es para otro email. Ingresá con la cuenta del email invitado.");
    }
}
