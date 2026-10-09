package com.lacivita.turnos.catalog.domain;

import com.lacivita.turnos.shared.domain.RuleViolationException;

public class NoPendingPriceException extends RuleViolationException {

    public NoPendingPriceException() {
        super("no_pending_price", "No hay un precio esperando aprobación.");
    }
}
