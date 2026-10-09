package com.lacivita.turnos.catalog.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

public interface ComboRepository extends Repository<Combo, UUID> {

    Combo save(Combo combo);

    Optional<Combo> findById(UUID id);

    Page<Combo> findByBusinessId(UUID businessId, Pageable pageable);

    List<Combo> findAllByBusinessIdAndActiveTrueOrderByNameAsc(UUID businessId);

    default Combo require(UUID id) {
        return findById(id).orElseThrow(ComboNotFoundException::new);
    }
}
