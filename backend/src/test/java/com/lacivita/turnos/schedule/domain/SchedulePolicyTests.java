package com.lacivita.turnos.schedule.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lacivita.turnos.shared.security.BusinessMembership;
import com.lacivita.turnos.shared.security.BusinessRole;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SchedulePolicyTests {

    static final UUID CENTRO = UUID.randomUUID();
    static final UUID NORTE = UUID.randomUUID();
    static final UUID ACTOR = UUID.randomUUID();
    static final UUID OTHER = UUID.randomUUID();

    static final BusinessMembership OWNER = new BusinessMembership(BusinessRole.OWNER, Set.of());
    static final BusinessMembership MANAGER_CENTRO = new BusinessMembership(BusinessRole.MANAGER, Set.of(CENTRO));
    static final BusinessMembership BARBER_CENTRO = new BusinessMembership(BusinessRole.BARBER, Set.of(CENTRO));
    static final BusinessMembership BARBER_NORTE = new BusinessMembership(BusinessRole.BARBER, Set.of(NORTE));

    @Test
    void everyoneManagesTheirOwnScheduleAndTheOwnerEveryones() {
        assertThatCode(() -> SchedulePolicy.checkCanManageBarber(ACTOR, BARBER_CENTRO, ACTOR, BARBER_CENTRO))
                .doesNotThrowAnyException();
        assertThatCode(() -> SchedulePolicy.checkCanManageBarber(ACTOR, OWNER, OTHER, MANAGER_CENTRO))
                .doesNotThrowAnyException();
    }

    @Test
    void aManagerManagesOnlyBarbersOfTheirBranches() {
        assertThatCode(() -> SchedulePolicy.checkCanManageBarber(ACTOR, MANAGER_CENTRO, OTHER, BARBER_CENTRO))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> SchedulePolicy.checkCanManageBarber(ACTOR, MANAGER_CENTRO, OTHER, BARBER_NORTE))
                .isInstanceOf(ScheduleActionNotAllowedException.class);
        assertThatThrownBy(() -> SchedulePolicy.checkCanManageBarber(ACTOR, BARBER_CENTRO, OTHER, BARBER_CENTRO))
                .isInstanceOf(ScheduleActionNotAllowedException.class);
    }

    @Test
    void forHoursAtABranchTheManagerOnlyNeedsThatBranch() {
        var barberInBoth = new BusinessMembership(BusinessRole.BARBER, Set.of(CENTRO, NORTE));

        assertThatCode(() -> SchedulePolicy.checkCanManageBarberAt(ACTOR, MANAGER_CENTRO, OTHER, barberInBoth, CENTRO))
                .doesNotThrowAnyException();
        assertThatThrownBy(
                        () -> SchedulePolicy.checkCanManageBarberAt(ACTOR, MANAGER_CENTRO, OTHER, barberInBoth, NORTE))
                .isInstanceOf(ScheduleActionNotAllowedException.class);
        assertThatThrownBy(() ->
                        SchedulePolicy.checkCanManageBarberAt(ACTOR, MANAGER_CENTRO, OTHER, MANAGER_CENTRO, CENTRO))
                .isInstanceOf(ScheduleActionNotAllowedException.class);
    }

    @Test
    void branchesAreManagedByTheirManagersAndTheWholeBusinessOnlyByTheOwner() {
        assertThatCode(() -> SchedulePolicy.checkCanManageBranch(MANAGER_CENTRO, CENTRO))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> SchedulePolicy.checkCanManageBranch(MANAGER_CENTRO, NORTE))
                .isInstanceOf(ScheduleActionNotAllowedException.class);
        assertThatThrownBy(() -> SchedulePolicy.checkCanManageBranch(MANAGER_CENTRO, null))
                .isInstanceOf(ScheduleActionNotAllowedException.class);
        assertThatThrownBy(() -> SchedulePolicy.checkCanManageBranch(BARBER_CENTRO, CENTRO))
                .isInstanceOf(ScheduleActionNotAllowedException.class);
        assertThatCode(() -> SchedulePolicy.checkCanManageBranch(OWNER, null)).doesNotThrowAnyException();
    }

    @Test
    void aBarberOnlyHasHoursWhereTheyWork() {
        assertThatCode(() -> SchedulePolicy.checkWorksAt(BARBER_CENTRO, CENTRO)).doesNotThrowAnyException();
        assertThatThrownBy(() -> SchedulePolicy.checkWorksAt(BARBER_CENTRO, NORTE))
                .isInstanceOf(BarberNotInBranchException.class);
    }
}
