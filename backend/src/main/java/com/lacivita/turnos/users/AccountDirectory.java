package com.lacivita.turnos.users;

import com.lacivita.turnos.shared.domain.Email;
import java.util.Optional;
import java.util.UUID;

/** Consultas sobre cuentas que pueden hacer otros módulos. */
public interface AccountDirectory {

    /** {@code true} si la cuenta existe y confirmó su email. */
    boolean hasVerifiedEmail(UUID userId);

    /** Email de la cuenta, por ejemplo para avisarle a un profesional de un turno nuevo. */
    Optional<Email> emailOf(UUID userId);
}
