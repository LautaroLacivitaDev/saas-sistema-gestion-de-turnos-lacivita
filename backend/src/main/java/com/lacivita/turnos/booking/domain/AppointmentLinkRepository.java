package com.lacivita.turnos.booking.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface AppointmentLinkRepository extends Repository<AppointmentLink, String> {

    AppointmentLink save(AppointmentLink link);

    Optional<AppointmentLink> findById(String tokenHash);

    /** Negocio del turno de un link. Se consulta como operación de sistema, antes de saber el negocio. */
    @Query("select l.businessId from AppointmentLink l where l.tokenHash = :hash")
    Optional<UUID> findBusinessIdByTokenHash(@Param("hash") String hash);

    default UUID requireAppointmentId(ManageToken token) {
        return findById(token.hash())
                .map(AppointmentLink::appointmentId)
                .orElseThrow(AppointmentNotFoundException::new);
    }
}
