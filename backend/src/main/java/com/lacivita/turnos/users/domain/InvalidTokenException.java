package com.lacivita.turnos.users.domain;

import com.lacivita.turnos.shared.domain.RuleViolationException;

/**
 * El link no sirve. No se distingue si no existe, venció o ya se usó: dar ese detalle ayudaría a
 * adivinar tokens.
 */
public class InvalidTokenException extends RuleViolationException {

    public InvalidTokenException() {
        super("invalid_token", "El link no es válido o ya venció. Pedí uno nuevo.");
    }
}
