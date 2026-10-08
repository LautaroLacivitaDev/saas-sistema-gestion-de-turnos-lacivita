package com.lacivita.turnos.users.application;

import com.lacivita.turnos.business.BusinessDirectory;
import com.lacivita.turnos.business.BusinessSummary;
import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.domain.InvalidValueException;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.security.BusinessRole;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import com.lacivita.turnos.users.application.TeamViews.InvitationView;
import com.lacivita.turnos.users.domain.AlreadyMemberException;
import com.lacivita.turnos.users.domain.Invitation;
import com.lacivita.turnos.users.domain.InvitationRepository;
import com.lacivita.turnos.users.domain.MembershipRepository;
import com.lacivita.turnos.users.domain.TeamPolicy;
import com.lacivita.turnos.users.domain.TokenSecret;
import com.lacivita.turnos.users.domain.UserRepository;
import java.time.Clock;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Invitaciones al equipo: las envían el dueño (gerentes y barberos) y los gerentes (barberos). */
@Service
public class TeamInvitations {

    private final InvitationRepository invitations;
    private final MembershipRepository memberships;
    private final UserRepository users;
    private final BusinessDirectory businesses;
    private final TeamActors actors;
    private final TeamEmails emails;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    TeamInvitations(
            InvitationRepository invitations,
            MembershipRepository memberships,
            UserRepository users,
            BusinessDirectory businesses,
            TeamActors actors,
            TeamEmails emails,
            ApplicationEventPublisher events,
            Clock clock) {
        this.invitations = invitations;
        this.memberships = memberships;
        this.users = users;
        this.businesses = businesses;
        this.actors = actors;
        this.emails = emails;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Invita a una persona por email. Si ya había una invitación abierta para ese email, la reemplaza: el
     * link anterior deja de funcionar.
     */
    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
    public InvitationView invite(
            @BusinessId UUID businessId,
            AuthenticatedUser inviter,
            Email email,
            BusinessRole role,
            Set<UUID> branchIds) {
        TeamPolicy.checkCanInvite(actors.actorIn(inviter, businessId), role, branchIds);
        if (!businesses.branchesBelongTo(businessId, branchIds)) {
            throw new InvalidValueException("invalid_branches", "Alguna de las sucursales no es de este negocio.");
        }
        if (isAlreadyMember(email, businessId)) {
            throw new AlreadyMemberException();
        }
        var now = clock.instant();
        invitations.findOpenByEmail(businessId, email).ifPresent(previous -> {
            previous.revoke(now);
            // La revocación se escribe antes de insertar la nueva: hay un índice único de invitación abierta.
            invitations.flush();
        });

        var secret = TokenSecret.generate();
        var invitation =
                invitations.save(Invitation.issue(businessId, email, role, branchIds, inviter.id(), secret, now));
        emails.sendInvitation(email, businessName(businessId), role, secret);
        events.publishEvent(
                new TeamEvents.MemberInvited(businessId, invitation.getId(), email.value(), role, branchIds));
        return toView(invitation);
    }

    @BusinessScoped
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
    public Page<InvitationView> pending(@BusinessId UUID businessId, Pageable pageable) {
        return invitations.findPending(businessId, clock.instant(), pageable).map(TeamInvitations::toView);
    }

    /** Revoca una invitación. Con las mismas reglas que para invitar: un gerente solo revoca las de barberos. */
    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
    public void revoke(@BusinessId UUID businessId, AuthenticatedUser actor, UUID invitationId) {
        var invitation = invitations.require(businessId, invitationId);
        TeamPolicy.checkCanInvite(actors.actorIn(actor, businessId), invitation.getRole(), invitation.getBranchIds());
        invitation.revoke(clock.instant());
        events.publishEvent(new TeamEvents.InvitationRevoked(businessId, invitationId));
    }

    private boolean isAlreadyMember(Email email, UUID businessId) {
        return users.findByEmail(email)
                .map(user -> memberships.existsByUserIdAndBusinessId(user.getId(), businessId))
                .orElse(false);
    }

    private String businessName(UUID businessId) {
        return businesses.find(businessId).map(BusinessSummary::name).orElseThrow();
    }

    static InvitationView toView(Invitation invitation) {
        return new InvitationView(
                invitation.getId(),
                invitation.getEmail().value(),
                invitation.getRole().name(),
                invitation.getBranchIds(),
                invitation.getExpiresAt());
    }
}
