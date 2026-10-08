package com.lacivita.turnos.business.application;

import com.lacivita.turnos.business.application.BusinessViews.BranchView;
import com.lacivita.turnos.business.domain.Branch;
import com.lacivita.turnos.business.domain.BranchDetails;
import com.lacivita.turnos.business.domain.BranchRepository;
import com.lacivita.turnos.business.domain.BusinessRepository;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sucursales: las crea y edita el dueño; las ve todo el equipo. */
@Service
public class BranchManagement {

    private final BranchRepository branches;
    private final BusinessRepository businesses;
    private final ApplicationEventPublisher events;
    private final BusinessMapper mapper;
    private final Clock clock;

    BranchManagement(
            BranchRepository branches,
            BusinessRepository businesses,
            ApplicationEventPublisher events,
            BusinessMapper mapper,
            Clock clock) {
        this.branches = branches;
        this.businesses = businesses;
        this.events = events;
        this.mapper = mapper;
        this.clock = clock;
    }

    @BusinessScoped
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public List<BranchView> list(@BusinessId UUID businessId) {
        return branches.findAllByBusinessIdOrderByCreatedAtAsc(businessId).stream()
                .map(mapper::toView)
                .toList();
    }

    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'OWNER')")
    public BranchView open(@BusinessId UUID businessId, BranchDetails details) {
        businesses.require(businessId);
        var branch = branches.save(Branch.open(businessId, details, clock.instant()));
        events.publishEvent(new BusinessEvents.BranchOpened(businessId, branch.getId(), details));
        return mapper.toView(branch);
    }

    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'OWNER')")
    public BranchView update(@BusinessId UUID businessId, UUID branchId, BranchDetails details) {
        var branch = branches.require(businessId, branchId);
        var before = branch.details();
        branch.update(details, clock.instant());
        events.publishEvent(new BusinessEvents.BranchUpdated(businessId, branchId, before, details));
        return mapper.toView(branch);
    }
}
