package com.lacivita.turnos.users.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lacivita.turnos.shared.security.BusinessMembership;
import com.lacivita.turnos.shared.security.BusinessRole;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TeamPolicyTests {

    static final UUID BUSINESS = UUID.randomUUID();
    static final UUID CENTRO = UUID.randomUUID();
    static final UUID NORTE = UUID.randomUUID();

    static final BusinessMembership OWNER = new BusinessMembership(BusinessRole.OWNER, Set.of());
    static final BusinessMembership MANAGER_CENTRO = new BusinessMembership(BusinessRole.MANAGER, Set.of(CENTRO));
    static final BusinessMembership BARBER_CENTRO = new BusinessMembership(BusinessRole.BARBER, Set.of(CENTRO));

    @Nested
    class Inviting {

        @Test
        void theOwnerInvitesManagersAndBarbersToAnyBranch() {
            assertThatCode(() -> TeamPolicy.checkCanInvite(OWNER, BusinessRole.MANAGER, Set.of(NORTE)))
                    .doesNotThrowAnyException();
            assertThatCode(() -> TeamPolicy.checkCanInvite(OWNER, BusinessRole.BARBER, Set.of(CENTRO, NORTE)))
                    .doesNotThrowAnyException();
        }

        @Test
        void aManagerInvitesBarbersOnlyToTheirOwnBranches() {
            assertThatCode(() -> TeamPolicy.checkCanInvite(MANAGER_CENTRO, BusinessRole.BARBER, Set.of(CENTRO)))
                    .doesNotThrowAnyException();
            assertThatThrownBy(() -> TeamPolicy.checkCanInvite(MANAGER_CENTRO, BusinessRole.BARBER, Set.of(NORTE)))
                    .isInstanceOf(TeamActionNotAllowedException.class);
        }

        @Test
        void aManagerCannotInviteManagers() {
            assertThatThrownBy(() -> TeamPolicy.checkCanInvite(MANAGER_CENTRO, BusinessRole.MANAGER, Set.of(CENTRO)))
                    .isInstanceOf(TeamActionNotAllowedException.class);
        }

        @Test
        void barbersCannotInvite() {
            assertThatThrownBy(() -> TeamPolicy.checkCanInvite(BARBER_CENTRO, BusinessRole.BARBER, Set.of(CENTRO)))
                    .isInstanceOf(TeamActionNotAllowedException.class);
        }
    }

    @Nested
    class Managing {

        @Test
        void nobodyCanModifyTheOwner() {
            var owner = Membership.grantOwner(UUID.randomUUID(), BUSINESS, Instant.now());

            assertThatThrownBy(() -> TeamPolicy.checkCanManage(OWNER, owner))
                    .isInstanceOf(TeamActionNotAllowedException.class);
            assertThatThrownBy(() -> TeamPolicy.checkCanChangeRole(OWNER, owner))
                    .isInstanceOf(TeamActionNotAllowedException.class);
        }

        @Test
        void aManagerManagesBarbersOfTheirBranchesButNotOtherManagers() {
            var barberCentro = staff(BusinessRole.BARBER, CENTRO);
            var barberNorte = staff(BusinessRole.BARBER, NORTE);
            var otherManager = staff(BusinessRole.MANAGER, CENTRO);

            assertThatCode(() -> TeamPolicy.checkCanManage(MANAGER_CENTRO, barberCentro))
                    .doesNotThrowAnyException();
            assertThatThrownBy(() -> TeamPolicy.checkCanManage(MANAGER_CENTRO, barberNorte))
                    .isInstanceOf(TeamActionNotAllowedException.class);
            assertThatThrownBy(() -> TeamPolicy.checkCanManage(MANAGER_CENTRO, otherManager))
                    .isInstanceOf(TeamActionNotAllowedException.class);
        }

        @Test
        void onlyTheOwnerChangesRoles() {
            var barber = staff(BusinessRole.BARBER, CENTRO);

            assertThatCode(() -> TeamPolicy.checkCanChangeRole(OWNER, barber)).doesNotThrowAnyException();
            assertThatThrownBy(() -> TeamPolicy.checkCanChangeRole(MANAGER_CENTRO, barber))
                    .isInstanceOf(TeamActionNotAllowedException.class);
        }

        @Test
        void aManagerCannotMoveABarberOutsideTheirBranches() {
            var barber = staff(BusinessRole.BARBER, CENTRO);

            assertThatThrownBy(() -> TeamPolicy.checkCanAssignBranches(MANAGER_CENTRO, barber, Set.of(NORTE)))
                    .isInstanceOf(TeamActionNotAllowedException.class);
        }
    }

    @Test
    void theOwnerRoleCannotBeAssignedOrChanged() {
        var barber = staff(BusinessRole.BARBER, CENTRO);
        var owner = Membership.grantOwner(UUID.randomUUID(), BUSINESS, Instant.now());

        assertThatThrownBy(() -> barber.changeRole(BusinessRole.OWNER))
                .isInstanceOf(com.lacivita.turnos.shared.domain.InvalidValueException.class);
        assertThatThrownBy(() -> owner.changeRole(BusinessRole.MANAGER))
                .isInstanceOf(TeamActionNotAllowedException.class);
    }

    private static Membership staff(BusinessRole role, UUID branch) {
        return Membership.join(UUID.randomUUID(), BUSINESS, role, Set.of(branch), Instant.now());
    }
}
