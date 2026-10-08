package com.lacivita.turnos.shared.security;

/**
 * Convierte una identidad externa en una cuenta propia: la encuentra, la vincula o la crea. Lo
 * implementa el módulo de usuarios.
 */
public interface ExternalIdentityResolver {

    /**
     * @throws com.lacivita.turnos.shared.domain.DomainException si la identidad no se puede aceptar (por
     *     ejemplo, el proveedor no verificó el email)
     */
    AuthenticatedUser resolve(ExternalIdentity identity);
}
