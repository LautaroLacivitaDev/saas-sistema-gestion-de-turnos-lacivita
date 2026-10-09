package com.lacivita.turnos.users.application;

import com.lacivita.turnos.business.BusinessDirectory;
import com.lacivita.turnos.shared.domain.InvalidValueException;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.security.BusinessRole;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import com.lacivita.turnos.users.MemberLeft;
import com.lacivita.turnos.users.application.TeamViews.MemberView;
import com.lacivita.turnos.users.domain.Membership;
import com.lacivita.turnos.users.domain.MembershipRepository;
import com.lacivita.turnos.users.domain.TeamPolicy;
import com.lacivita.turnos.users.domain.User;
import com.lacivita.turnos.users.domain.UserRepository;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestión del equipo: listado, roles, sucursales asignadas y bajas. */
@Service
public class TeamMembers {

    private final MembershipRepository memberships;
    private final UserRepository users;
    private final BusinessDirectory businesses;
    private final TeamActors actors;
    private final ApplicationEventPublisher events;

    TeamMembers(
            MembershipRepository memberships,
            UserRepository users,
            BusinessDirectory businesses,
            TeamActors actors,
            ApplicationEventPublisher events) {
        this.memberships = memberships;
        this.users = users;
        this.businesses = businesses;
        this.actors = actors;
        this.events = events;
    }

    @BusinessScoped
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
    public Page<MemberView> list(@BusinessId UUID businessId, Pageable pageable) {
        Page<Membership> page = memberships.findByBusinessId(businessId, pageable);
        // Una sola consulta para todas las personas de la página (sin N+1).
        Map<UUID, User> people = users
                .findAllByIdIn(page.map(Membership::getUserId).toSet())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return page.map(membership -> toView(membership, people.get(membership.getUserId())));
    }

    /** Nombrar o quitar gerentes (pasar entre gerente y barbero). Solo el dueño. */
    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'OWNER')")
    public MemberView changeRole(@BusinessId UUID businessId, AuthenticatedUser actor, UUID userId, BusinessRole role) {
        var membership = memberships.requireMember(userId, businessId);
        TeamPolicy.checkCanChangeRole(actors.actorIn(actor, businessId), membership);
        var before = membership.getRole();
        membership.changeRole(role);
        events.publishEvent(new TeamEvents.MemberRoleChanged(businessId, userId, before, role));
        return toView(membership, users.require(userId));
    }

    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
    public MemberView assignBranches(
            @BusinessId UUID businessId, AuthenticatedUser actor, UUID userId, Set<UUID> branchIds) {
        var membership = memberships.requireMember(userId, businessId);
        TeamPolicy.checkCanAssignBranches(actors.actorIn(actor, businessId), membership, branchIds);
        if (!businesses.branchesBelongTo(businessId, branchIds)) {
            throw new InvalidValueException("invalid_branches", "Alguna de las sucursales no es de este negocio.");
        }
        var before = membership.getBranchIds();
        membership.assignBranches(branchIds);
        events.publishEvent(new TeamEvents.MemberBranchesChanged(businessId, userId, before, branchIds));
        return toView(membership, users.require(userId));
    }

    /** Da de baja a un miembro. El dueño puede dar de baja a cualquiera; el gerente, solo a barberos. */
    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
    public void remove(@BusinessId UUID businessId, AuthenticatedUser actor, UUID userId) {
        var membership = memberships.requireMember(userId, businessId);
        TeamPolicy.checkCanManage(actors.actorIn(actor, businessId), membership);
        memberships.delete(membership);
        events.publishEvent(new TeamEvents.MemberRemoved(businessId, userId, membership.getRole()));
        events.publishEvent(new MemberLeft(businessId, userId));
    }

    private static MemberView toView(Membership membership, User user) {
        return new MemberView(
                membership.getUserId(),
                user.getName(),
                user.getEmail().value(),
                membership.getRole().name(),
                membership.getBranchIds());
    }
}
