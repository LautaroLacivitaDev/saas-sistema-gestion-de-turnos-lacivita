package com.lacivita.turnos.business.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface BusinessRepository extends Repository<Business, UUID> {

    Business save(Business business);

    /** Guarda y escribe enseguida, para detectar un slug repetido dentro del caso de uso. */
    Business saveAndFlush(Business business);

    /** Escribe los cambios pendientes (por ejemplo, el slug nuevo de un negocio ya cargado). */
    void flush();

    Optional<Business> findById(UUID id);

    List<Business> findAllByIdIn(Collection<UUID> ids);

    default Business require(UUID id) {
        return findById(id).orElseThrow(BusinessNotFoundException::new);
    }
}
