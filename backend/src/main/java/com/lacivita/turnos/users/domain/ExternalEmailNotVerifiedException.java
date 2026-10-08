package com.lacivita.turnos.users.domain;

import com.lacivita.turnos.shared.domain.RuleViolationException;

/**
 * El proveedor externo no confirmó que la persona controla el email. Sin esa confirmación no se puede
 * vincular ni crear la cuenta: cualquiera podría declarar el email de otra persona.
 */
public class ExternalEmailNotVerifiedException extends RuleViolationException {

    public ExternalEmailNotVerifiedException() {
        super("external_email_not_verified", "Tu cuenta externa no tiene el email verificado.");
    }
}
