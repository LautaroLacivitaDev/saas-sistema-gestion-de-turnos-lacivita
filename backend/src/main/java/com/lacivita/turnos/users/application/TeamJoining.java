package com.lacivita.turnos.users.application;

import com.lacivita.turnos.business.BusinessDirectory;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import com.lacivita.turnos.users.application.TeamViews.MembershipView;
import com.lacivita.turnos.users.domain.AlreadyMemberException;
import com.lacivita.turnos.users.domain.InvitationNoLongerValidException;
import com.lacivita.turnos.users.domain.InvitationRepository;
import com.lacivita.turnos.users.domain.MembershipRepository;
import com.lacivita.turnos.users.domain.TokenSecret;
import com.lacivita.turnos.users.domain.UserRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Segunda parte de aceptar una invitación: ya dentro del negocio correcto. */
@Component
class TeamJoining {

    private final InvitationRepository invitations;
    private final MembershipRepository memberships;
    private final UserRepository users;
    private final BusinessDirectory businesses;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    TeamJoining(
            InvitationRepository invitations,
            MembershipRepository memberships,
            UserRepository users,
            BusinessDirectory businesses,
            ApplicationEventPublisher events,
            Clock clock) {
        this.invitations = invitations;
        this.memberships = memberships;
        this.users = users;
        this.businesses = businesses;
        this.events = events;
        this.clock = clock;
    }

    @BusinessScoped
    @Transactional
    public MembershipView join(@BusinessId UUID businessId, TokenSecret secret, AuthenticatedUser user) {
        var invitation = invitations.findByTokenHash(secret.hash()).orElseThrow(InvitationNoLongerValidException::new);
        var account = users.require(user.id());
        var now = clock.instant();
        // Primero la invitación: una ya usada responde que no es válida, aunque la persona ya sea miembro.
        var accepted = invitation.accept(account.getId(), account.getEmail(), now);
        if (memberships.existsByUserIdAndBusinessId(account.getId(), businessId)) {
            throw new AlreadyMemberException();
        }
        var membership = memberships.save(accepted);
        // El link llegó a ese email: usarlo prueba que la persona lo controla.
        account.verifyEmail(now);
        events.publishEvent(new TeamEvents.MemberJoined(
                businessId, account.getId(), membership.getRole(), membership.getBranchIds()));

        var business = businesses.find(businessId).orElseThrow();
        return new MembershipView(
                businessId,
                business.name(),
                business.slug(),
                membership.getRole().name(),
                membership.getBranchIds());
    }
}
