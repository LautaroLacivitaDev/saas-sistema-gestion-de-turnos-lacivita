/**
 * Infraestructura común: seguridad, aislamiento entre negocios, auditoría y manejo de errores.
 *
 * <p>Es un módulo abierto: los demás módulos pueden usar cualquiera de sus tipos. No debe depender de
 * ningún módulo de negocio.
 */
@ApplicationModule(displayName = "Compartido", type = ApplicationModule.Type.OPEN)
package com.lacivita.turnos.shared;

import org.springframework.modulith.ApplicationModule;
