package com.lacivita.turnos.users.application;

import com.lacivita.turnos.shared.security.AuthenticatedUser;

/**
 * Resultado de un login exitoso: el principal para guardar en la sesión y los datos de la cuenta para
 * responderle al frontend.
 */
public record SignIn(AuthenticatedUser principal, AccountView account) {}
