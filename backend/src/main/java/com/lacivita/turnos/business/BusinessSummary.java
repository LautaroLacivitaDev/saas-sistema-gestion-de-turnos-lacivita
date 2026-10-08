package com.lacivita.turnos.business;

import java.util.UUID;

/** Identificación de un negocio para otros módulos. */
public record BusinessSummary(UUID id, String name, String slug) {}
