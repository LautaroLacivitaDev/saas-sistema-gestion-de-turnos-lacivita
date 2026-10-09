package com.lacivita.turnos.business;

import java.time.ZoneId;
import java.util.UUID;

/** Identificación de una sucursal para otros módulos, con la zona horaria en la que atiende. */
public record BranchSummary(UUID id, UUID businessId, String name, ZoneId timeZone) {}
