package com.lacivita.turnos.business.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface SlugClaimRepository extends Repository<SlugClaim, String> {

    boolean existsById(String slug);

    /** Negocio que usa o usó ese slug (para la página pública y la redirección de links viejos). */
    @Query("select c.business from SlugClaim c where c.slug = :slug")
    Optional<Business> findBusinessBySlug(@Param("slug") String slug);
}
