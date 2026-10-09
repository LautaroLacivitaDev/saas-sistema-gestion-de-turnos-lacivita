package com.lacivita.turnos.catalog.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

public interface BarberServiceRepository extends Repository<BarberService, UUID> {

    BarberService save(BarberService offering);

    Optional<BarberService> findById(UUID id);

    Optional<BarberService> findByBarberIdAndServiceId(UUID barberId, UUID serviceId);

    /** Servicios de un barbero. Son pocos: se devuelven completos. */
    List<BarberService> findAllByBarberIdAndActiveTrue(UUID barberId);

    /** Lo que ofrece hoy cada barbero en esos servicios (para el catálogo público). */
    List<BarberService> findAllByServiceIdInAndActiveTrue(Collection<UUID> serviceIds);

    Page<BarberService> findByBusinessIdAndRequestedPriceIsNotNull(UUID businessId, Pageable pageable);

    default BarberService require(UUID id) {
        return findById(id).orElseThrow(OfferingNotFoundException::new);
    }

    default BarberService requireActive(UUID barberId, UUID serviceId) {
        return findByBarberIdAndServiceId(barberId, serviceId)
                .filter(BarberService::isActive)
                .orElseThrow(OfferingNotFoundException::new);
    }
}
