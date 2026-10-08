package com.lacivita.turnos.users.application;

import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.tenancy.TenantContext;
import com.lacivita.turnos.users.application.TeamViews.MembershipView;
import com.lacivita.turnos.users.domain.InvitationNoLongerValidException;
import com.lacivita.turnos.users.domain.TokenSecret;
import org.springframework.stereotype.Service;

/**
 * Aceptación de una invitación con el token del email, en dos pasos:
 *
 * <ol>
 *   <li>Como operación de sistema, averigua a qué negocio pertenece el token (es lo único que se lee sin
 *       aislamiento).
 *   <li>Dentro de ese negocio, valida la invitación y crea la membresía.
 * </ol>
 */
@Service
public class InvitationAcceptance {

    private final InvitationLookup lookup;
    private final TeamJoining joining;

    InvitationAcceptance(InvitationLookup lookup, TeamJoining joining) {
        this.lookup = lookup;
        this.joining = joining;
    }

    public MembershipView accept(String rawToken, AuthenticatedUser user) {
        var secret = new TokenSecret(rawToken);
        var businessId = TenantContext.callAsSystem(
                        "aceptar invitación: averiguar el negocio del token", () -> lookup.businessOf(secret))
                .orElseThrow(InvitationNoLongerValidException::new);
        return joining.join(businessId, secret, user);
    }
}
