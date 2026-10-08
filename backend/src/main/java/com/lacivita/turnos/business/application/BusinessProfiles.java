package com.lacivita.turnos.business.application;

import com.lacivita.turnos.business.application.BusinessViews.BusinessView;
import com.lacivita.turnos.business.domain.BusinessProfile;
import com.lacivita.turnos.business.domain.BusinessRepository;
import com.lacivita.turnos.business.domain.Slug;
import com.lacivita.turnos.business.domain.SlugTakenException;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Datos del negocio: consulta para el equipo y cambios solo para el dueño. */
@Service
public class BusinessProfiles {

    private final BusinessRepository businesses;
    private final SlugGuard slugs;
    private final ApplicationEventPublisher events;
    private final BusinessMapper mapper;
    private final Clock clock;

    BusinessProfiles(
            BusinessRepository businesses,
            SlugGuard slugs,
            ApplicationEventPublisher events,
            BusinessMapper mapper,
            Clock clock) {
        this.businesses = businesses;
        this.slugs = slugs;
        this.events = events;
        this.mapper = mapper;
        this.clock = clock;
    }

    @BusinessScoped
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public BusinessView view(@BusinessId UUID businessId) {
        return mapper.toView(businesses.require(businessId));
    }

    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'OWNER')")
    public BusinessView updateProfile(@BusinessId UUID businessId, BusinessProfile profile) {
        var business = businesses.require(businessId);
        var before = business.profile();
        business.updateProfile(profile, clock.instant());
        events.publishEvent(new BusinessEvents.ProfileChanged(businessId, before, business.profile()));
        return mapper.toView(business);
    }

    /** Cambia el link. El anterior queda reservado para el negocio y redirige al nuevo. */
    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'OWNER')")
    public BusinessView changeSlug(@BusinessId UUID businessId, Slug newSlug) {
        var business = businesses.require(businessId);
        if (business.hasSlug(newSlug)) {
            return mapper.toView(business);
        }
        if (!business.hasUsed(newSlug) && slugs.isTaken(newSlug.value())) {
            throw new SlugTakenException();
        }
        String before = business.getSlug();
        business.changeSlug(newSlug, clock.instant());
        slugs.flushClaims();
        events.publishEvent(new BusinessEvents.SlugChanged(businessId, before, newSlug.value()));
        return mapper.toView(business);
    }
}
