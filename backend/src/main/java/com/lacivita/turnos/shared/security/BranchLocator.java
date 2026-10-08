package com.lacivita.turnos.shared.security;

import java.util.Optional;
import java.util.UUID;

/** Informa a qué negocio pertenece una sucursal. Lo implementa el módulo de negocios. */
public interface BranchLocator {

    Optional<UUID> businessOf(UUID branchId);
}
