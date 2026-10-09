package com.lacivita.turnos.booking.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface GuestCheckRepository extends Repository<GuestCheck, UUID> {

    GuestCheck save(GuestCheck check);

    void delete(GuestCheck check);

    Optional<GuestCheck> findById(UUID appointmentId);
}
