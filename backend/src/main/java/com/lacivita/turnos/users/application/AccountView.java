package com.lacivita.turnos.users.application;

import java.util.UUID;

/** Datos de la cuenta que se muestran a su dueño. Nunca incluye secretos. */
public record AccountView(
        UUID id, String name, String email, boolean emailVerified, boolean hasPassword, String platformRole) {}
