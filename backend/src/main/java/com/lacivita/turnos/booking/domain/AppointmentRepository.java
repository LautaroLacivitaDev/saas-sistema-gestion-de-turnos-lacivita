package com.lacivita.turnos.booking.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends Repository<Appointment, UUID> {

    /** Guarda y escribe enseguida: una superposición la detecta la base dentro del caso de uso. */
    Appointment saveAndFlush(Appointment appointment);

    /** Escribe los cambios de un turno ya cargado (por ejemplo, un horario nuevo). */
    void flush();

    Optional<Appointment> findById(UUID id);

    Optional<Appointment> findByManageTokenHash(String manageTokenHash);

    /** Negocio del turno de un link. Se consulta como operación de sistema, antes de saber el negocio. */
    @Query("select a.businessId from Appointment a where a.manageTokenHash = :hash")
    Optional<UUID> findBusinessIdByManageTokenHash(@Param("hash") String hash);

    /** Turnos que ocupan el horario del profesional en el período (los HOLD, solo si no vencieron). */
    @Query("""
            select a from Appointment a
            where a.barberId = :barberId
              and a.status in :statuses
              and (a.holdExpiresAt is null or a.holdExpiresAt > :now)
              and a.startsAt < :to and a.endsAt > :from
            """)
    List<Appointment> findOccupying(
            @Param("barberId") UUID barberId,
            @Param("statuses") Collection<AppointmentStatus> statuses,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("now") Instant now);

    /** Turnos de la agenda del período, de todas las sucursales, ordenados por hora. */
    @Query("""
            select a from Appointment a
            where a.status in :statuses and a.startsAt < :to and a.endsAt > :from
            order by a.startsAt
            """)
    List<Appointment> findInPeriod(
            @Param("statuses") Collection<AppointmentStatus> statuses,
            @Param("from") Instant from,
            @Param("to") Instant to);

    /** Borra los HOLD vencidos del profesional, para que no ocupen un horario que se va a reservar. */
    @Modifying(flushAutomatically = true)
    @Query("""
            delete from Appointment a
            where a.barberId = :barberId and a.status = com.lacivita.turnos.booking.domain.AppointmentStatus.HOLD
              and a.holdExpiresAt <= :now
            """)
    void deleteExpiredHolds(@Param("barberId") UUID barberId, @Param("now") Instant now);

    default Appointment require(UUID id) {
        return findById(id).orElseThrow(AppointmentNotFoundException::new);
    }

    default Appointment requireByToken(ManageToken token) {
        return findByManageTokenHash(token.hash()).orElseThrow(AppointmentNotFoundException::new);
    }
}
