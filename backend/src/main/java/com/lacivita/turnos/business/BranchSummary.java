package com.lacivita.turnos.business;

import java.time.ZoneId;
import java.util.UUID;

/**
 * Identificación de una sucursal para otros módulos, con la zona horaria en la que atiende.
 *
 * @param address dirección en una línea, por ejemplo "Av. Corrientes 1234, Almagro, CABA"
 */
public record BranchSummary(UUID id, UUID businessId, String name, String address, ZoneId timeZone) {}
