package com.lacivita.turnos.users.application;

import com.lacivita.turnos.users.domain.InvitationRepository;
import com.lacivita.turnos.users.domain.TokenSecret;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Averigua a qué negocio pertenece una invitación a partir de su token. Es la única consulta que mira
 * invitaciones de cualquier negocio, por eso se ejecuta como operación de sistema y devuelve solo el id.
 */
@Component
class InvitationLookup {

    private final InvitationRepository invitations;

    InvitationLookup(InvitationRepository invitations) {
        this.invitations = invitations;
    }

    @Transactional(readOnly = true)
    public Optional<UUID> businessOf(TokenSecret secret) {
        return invitations.findBusinessIdByTokenHash(secret.hash());
    }
}
