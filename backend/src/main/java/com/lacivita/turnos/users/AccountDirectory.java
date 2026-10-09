package com.lacivita.turnos.users;

import java.util.UUID;

/** Consultas sobre cuentas que pueden hacer otros módulos. */
public interface AccountDirectory {

    /** {@code true} si la cuenta existe y confirmó su email. */
    boolean hasVerifiedEmail(UUID userId);
}
