package com.lacivita.turnos.booking;

import com.lacivita.turnos.shared.security.AuthenticatedUser;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Consultas sobre los turnos que pueden hacer otros módulos. */
public interface AppointmentDirectory {

    /** El turno, si existe y es del negocio. Un {@code HOLD} no cuenta: todavía no es un turno. */
    Optional<AppointmentDetails> details(UUID businessId, UUID appointmentId);

    /** Turnos confirmados o a confirmar del profesional en el período, ordenados por hora. */
    List<AppointmentDetails> upcomingForBarber(UUID businessId, UUID barberId, Instant from, Instant to);

    /** {@code true} si la persona del equipo puede ver el turno en la agenda. */
    boolean isVisibleTo(UUID businessId, AuthenticatedUser actor, UUID appointmentId);

    /**
     * Crea un link nuevo para que el cliente vea, confirme, cancele o reprograme el turno sin iniciar sesión.
     * Cada email lleva el suyo; los anteriores siguen funcionando.
     *
     * @return dirección absoluta del frontend, con el token como parámetro {@code token}
     */
    String newManageLink(UUID businessId, UUID appointmentId);
}
