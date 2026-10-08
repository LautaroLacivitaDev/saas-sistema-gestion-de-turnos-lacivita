package com.lacivita.turnos.users.web;

import com.lacivita.turnos.shared.security.BusinessRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

/** Cuerpos de las solicitudes de gestión del equipo. */
final class TeamRequests {

    private TeamRequests() {}

    record Invite(
            @NotBlank(message = "Ingresá el email.") @Email(message = "El email no tiene un formato válido.")
            String email,

            @NotNull(message = "Elegí el rol.") BusinessRole role,

            @NotEmpty(message = "Asigná al menos una sucursal.") @Size(max = 50)
            Set<UUID> branchIds) {}

    record ChangeRole(@NotNull(message = "Elegí el rol.") BusinessRole role) {}

    record AssignBranches(
            @NotEmpty(message = "Asigná al menos una sucursal.") @Size(max = 50)
            Set<UUID> branchIds) {}

    record AcceptInvitation(
            @NotBlank(message = "Falta el token.") @Size(max = 200)
            String token) {}
}
