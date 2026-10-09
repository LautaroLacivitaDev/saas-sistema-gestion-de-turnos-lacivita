package com.lacivita.turnos.catalog.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface BarberProfileRepository extends Repository<BarberProfile, UUID> {

    BarberProfile save(BarberProfile profile);

    Optional<BarberProfile> findByBarberId(UUID barberId);

    List<BarberProfile> findAllByBarberIdIn(Collection<UUID> barberIds);

    default ProfessionalProfile profileOf(UUID barberId) {
        return findByBarberId(barberId).map(BarberProfile::profile).orElse(ProfessionalProfile.EMPTY);
    }
}
