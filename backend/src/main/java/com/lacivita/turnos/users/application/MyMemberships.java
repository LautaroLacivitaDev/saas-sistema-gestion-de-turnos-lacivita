package com.lacivita.turnos.users.application;

import com.lacivita.turnos.business.BusinessDirectory;
import com.lacivita.turnos.business.BusinessSummary;
import com.lacivita.turnos.users.application.TeamViews.MembershipView;
import com.lacivita.turnos.users.domain.Membership;
import com.lacivita.turnos.users.domain.MembershipRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Negocios en los que trabaja la persona con sesión iniciada (para elegir cuál administrar). */
@Service
public class MyMemberships {

    private final MembershipRepository memberships;
    private final BusinessDirectory businesses;

    MyMemberships(MembershipRepository memberships, BusinessDirectory businesses) {
        this.memberships = memberships;
        this.businesses = businesses;
    }

    @Transactional(readOnly = true)
    public List<MembershipView> of(UUID userId) {
        List<Membership> mine = memberships.findAllByUserIdOrderByCreatedAtAsc(userId);
        Map<UUID, BusinessSummary> byId =
                businesses.findAll(mine.stream().map(Membership::getBusinessId).toList()).stream()
                        .collect(Collectors.toMap(BusinessSummary::id, Function.identity()));
        return mine.stream()
                .map(membership -> {
                    var business = byId.get(membership.getBusinessId());
                    return new MembershipView(
                            business.id(),
                            business.name(),
                            business.slug(),
                            membership.getRole().name());
                })
                .toList();
    }
}
