package com.lacivita.turnos.business;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Consultas sobre negocios y sucursales que pueden hacer otros módulos. */
public interface BusinessDirectory {

    Optional<BusinessSummary> find(UUID businessId);

    List<BusinessSummary> findAll(Collection<UUID> businessIds);

    /** Negocio por su link actual o por uno que usó antes. */
    Optional<BusinessSummary> findBySlug(String slug);

    /** Sucursal del negocio; vacío si no existe o es de otro negocio. */
    Optional<BranchSummary> branch(UUID businessId, UUID branchId);

    /** {@code true} si todas las sucursales existen y pertenecen al negocio. */
    boolean branchesBelongTo(UUID businessId, Set<UUID> branchIds);
}
