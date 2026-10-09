package com.lacivita.turnos.schedule.application;

import com.lacivita.turnos.schedule.application.ScheduleViews.TimeBlockView;
import com.lacivita.turnos.schedule.domain.SchedulePolicy;
import com.lacivita.turnos.schedule.domain.TimeBlock;
import com.lacivita.turnos.schedule.domain.TimeBlockRepository;
import com.lacivita.turnos.shared.domain.TimeInterval;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.security.BusinessMembership;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bloqueos de horario. Los de un profesional los carga él mismo, el gerente de sus sucursales o el dueño;
 * los de una sucursal, su gerente o el dueño.
 */
@Service
public class TimeBlocks {

    private final TimeBlockRepository blocks;
    private final ScheduleActors actors;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    TimeBlocks(TimeBlockRepository blocks, ScheduleActors actors, ApplicationEventPublisher events, Clock clock) {
        this.blocks = blocks;
        this.actors = actors;
        this.events = events;
        this.clock = clock;
    }

    @BusinessScoped
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public Page<TimeBlockView> overlapping(@BusinessId UUID businessId, TimeInterval period, Pageable pageable) {
        return blocks.findOverlapping(period.start(), period.end(), pageable).map(TimeBlocks::toView);
    }

    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public TimeBlockView blockBarber(
            @BusinessId UUID businessId, AuthenticatedUser actor, UUID barberId, TimeInterval interval, String reason) {
        var barber = actors.barberIn(businessId, barberId);
        SchedulePolicy.checkCanManageBarber(
                actor.id(), actors.actorIn(actor, businessId), barberId, barber.membership());
        var block = blocks.save(TimeBlock.forBarber(businessId, barberId, interval, reason, clock.instant()));
        publish(businessId, block, true);
        return toView(block);
    }

    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
    public TimeBlockView blockBranch(
            @BusinessId UUID businessId, AuthenticatedUser actor, UUID branchId, TimeInterval interval, String reason) {
        actors.branchOf(businessId, branchId);
        SchedulePolicy.checkCanManageBranch(actors.actorIn(actor, businessId), branchId);
        var block = blocks.save(TimeBlock.forBranch(businessId, branchId, interval, reason, clock.instant()));
        publish(businessId, block, true);
        return toView(block);
    }

    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public void remove(@BusinessId UUID businessId, AuthenticatedUser actor, UUID blockId) {
        var block = blocks.require(blockId);
        BusinessMembership actorMembership = actors.actorIn(actor, businessId);
        if (block.barberId().isPresent()) {
            var barberId = block.barberId().get();
            var barber = actors.barberIn(businessId, barberId);
            SchedulePolicy.checkCanManageBarber(actor.id(), actorMembership, barberId, barber.membership());
        } else {
            SchedulePolicy.checkCanManageBranch(
                    actorMembership, block.branchId().orElseThrow());
        }
        blocks.delete(block);
        publish(businessId, block, false);
    }

    private void publish(UUID businessId, TimeBlock block, boolean added) {
        var interval = block.interval();
        events.publishEvent(new ScheduleEvents.TimeBlockChanged(
                businessId,
                block.getId(),
                block.barberId().orElse(null),
                block.branchId().orElse(null),
                interval.start(),
                interval.end(),
                added));
    }

    private static TimeBlockView toView(TimeBlock block) {
        var interval = block.interval();
        return new TimeBlockView(
                block.getId(),
                block.barberId().orElse(null),
                block.branchId().orElse(null),
                interval.start(),
                interval.end(),
                block.reason().orElse(null));
    }
}
