package com.lacivita.turnos.users.application;

import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import com.lacivita.turnos.users.TeamDirectory;
import com.lacivita.turnos.users.TeamMember;
import com.lacivita.turnos.users.domain.Membership;
import com.lacivita.turnos.users.domain.MembershipRepository;
import com.lacivita.turnos.users.domain.User;
import com.lacivita.turnos.users.domain.UserRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementa las consultas sobre el equipo que el módulo ofrece a otros módulos. */
@Service
class TeamDirectoryService implements TeamDirectory {

    private final MembershipRepository memberships;
    private final UserRepository users;

    TeamDirectoryService(MembershipRepository memberships, UserRepository users) {
        this.memberships = memberships;
        this.users = users;
    }

    @Override
    @BusinessScoped
    @Transactional(readOnly = true)
    public Optional<TeamMember> member(@BusinessId UUID businessId, UUID userId) {
        return memberships
                .findByUserIdAndBusinessId(userId, businessId)
                .map(membership -> toMember(membership, users.require(userId)));
    }

    @Override
    @BusinessScoped
    @Transactional(readOnly = true)
    public List<TeamMember> members(@BusinessId UUID businessId, Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        List<Membership> found = memberships.findAllByBusinessIdAndUserIdIn(businessId, userIds);
        Map<UUID, User> people = users
                .findAllByIdIn(found.stream().map(Membership::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return found.stream()
                .map(membership -> toMember(membership, people.get(membership.getUserId())))
                .toList();
    }

    private static TeamMember toMember(Membership membership, User user) {
        return new TeamMember(user.getId(), user.getName(), membership.toBusinessMembership());
    }
}
