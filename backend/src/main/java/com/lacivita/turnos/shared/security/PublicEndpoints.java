package com.lacivita.turnos.shared.security;

import java.util.List;

/**
 * Cada módulo con endpoints públicos registra un bean que implementa esta interfaz. Todo lo que no se
 * declare acá exige sesión.
 */
public interface PublicEndpoints {

    List<PublicEndpoint> publicEndpoints();
}
