package com.lacivita.turnos.users.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.domain.InvalidValueException;
import com.lacivita.turnos.shared.security.BusinessRole;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InvitationTests {

    static final Instant NOW = Instant.parse("2026-10-08T12:00:00Z");
    static final UUID BUSINESS = UUID.randomUUID();
    static final UUID BRANCH = UUID.randomUUID();
    static final Email EMAIL = new Email("barbero@example.com");

    @Test
    void acceptingCreatesAMembershipWithTheInvitedRoleAndBranches() {
        var invitation = invite(BusinessRole.BARBER);
        var userId = UUID.randomUUID();

        var membership = invitation.accept(userId, new Email("BARBERO@example.com"), NOW.plusSeconds(60));

        assertThat(membership.getUserId()).isEqualTo(userId);
        assertThat(membership.getBusinessId()).isEqualTo(BUSINESS);
        assertThat(membership.getRole()).isEqualTo(BusinessRole.BARBER);
        assertThat(membership.getBranchIds()).containsExactly(BRANCH);
        assertThat(invitation.isPending(NOW)).isFalse();
    }

    @Test
    void onlyTheInvitedEmailCanAccept() {
        var invitation = invite(BusinessRole.BARBER);

        assertThatThrownBy(() -> invitation.accept(UUID.randomUUID(), new Email("otra@example.com"), NOW))
                .isInstanceOf(InvitationEmailMismatchException.class);
    }

    @Test
    void anInvitationWorksOnlyOnce() {
        var invitation = invite(BusinessRole.BARBER);
        invitation.accept(UUID.randomUUID(), EMAIL, NOW);

        assertThatThrownBy(() -> invitation.accept(UUID.randomUUID(), EMAIL, NOW))
                .isInstanceOf(InvitationNoLongerValidException.class);
    }

    @Test
    void revokedOrExpiredInvitationsCannotBeAccepted() {
        var revoked = invite(BusinessRole.BARBER);
        revoked.revoke(NOW);
        var expired = invite(BusinessRole.BARBER);

        assertThatThrownBy(() -> revoked.accept(UUID.randomUUID(), EMAIL, NOW))
                .isInstanceOf(InvitationNoLongerValidException.class);
        assertThatThrownBy(() -> expired.accept(UUID.randomUUID(), EMAIL, NOW.plus(Invitation.VALIDITY)))
                .isInstanceOf(InvitationNoLongerValidException.class);
    }

    @Test
    void nobodyCanBeInvitedAsOwnerOrWithoutBranches() {
        assertThatThrownBy(() -> invite(BusinessRole.OWNER)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> Invitation.issue(
                        BUSINESS, EMAIL, BusinessRole.BARBER, Set.of(), UUID.randomUUID(), TokenSecret.generate(), NOW))
                .isInstanceOf(InvalidValueException.class);
    }

    private static Invitation invite(BusinessRole role) {
        return Invitation.issue(BUSINESS, EMAIL, role, Set.of(BRANCH), UUID.randomUUID(), TokenSecret.generate(), NOW);
    }
}
