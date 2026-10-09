package com.lacivita.turnos.catalog.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

public interface ServiceRepository extends Repository<Service, UUID> {

    Service save(Service service);

    /** Guarda y escribe enseguida, para detectar un nombre repetido dentro del caso de uso. */
    Service saveAndFlush(Service service);

    /** Escribe los cambios pendientes de un servicio ya cargado (por ejemplo, un nombre nuevo). */
    void flush();

    Optional<Service> findById(UUID id);

    List<Service> findAllByIdIn(Collection<UUID> ids);

    Page<Service> findByBusinessIdAndStatusIn(UUID businessId, Collection<ServiceStatus> statuses, Pageable pageable);

    List<Service> findAllByBusinessIdAndStatusOrderByCategoryAscNameAsc(UUID businessId, ServiceStatus status);

    default Service require(UUID id) {
        return findById(id).orElseThrow(ServiceNotFoundException::new);
    }
}
