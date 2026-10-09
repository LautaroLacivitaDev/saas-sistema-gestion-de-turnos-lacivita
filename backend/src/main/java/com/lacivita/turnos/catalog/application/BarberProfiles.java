package com.lacivita.turnos.catalog.application;

import com.lacivita.turnos.catalog.application.CatalogViews.ProfileView;
import com.lacivita.turnos.catalog.domain.BarberProfile;
import com.lacivita.turnos.catalog.domain.BarberProfileRepository;
import com.lacivita.turnos.catalog.domain.CatalogPolicy;
import com.lacivita.turnos.catalog.domain.ProfessionalProfile;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Perfil público de cada profesional: foto, descripción y especialidades. Lo edita el profesional, o quien
 * gestiona sus servicios (el gerente de sus sucursales o el dueño).
 */
@Service
public class BarberProfiles {

    private final BarberProfileRepository profiles;
    private final CatalogActors actors;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    BarberProfiles(
            BarberProfileRepository profiles, CatalogActors actors, ApplicationEventPublisher events, Clock clock) {
        this.profiles = profiles;
        this.actors = actors;
        this.events = events;
        this.clock = clock;
    }

    @BusinessScoped
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public ProfileView of(@BusinessId UUID businessId, UUID barberId) {
        actors.barberIn(businessId, barberId);
        return view(profiles.profileOf(barberId));
    }

    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public ProfileView change(
            @BusinessId UUID businessId, AuthenticatedUser actor, UUID barberId, ProfessionalProfile profile) {
        var barber = actors.barberIn(businessId, barberId);
        CatalogPolicy.checkCanManageOfferingsOf(
                actor.id(), actors.actorIn(actor, businessId), barberId, barber.membership());
        var now = clock.instant();
        profiles.findByBarberId(barberId)
                .ifPresentOrElse(
                        existing -> existing.change(profile, now),
                        () -> profiles.save(BarberProfile.of(businessId, barberId, profile, now)));
        events.publishEvent(new CatalogEvents.ProfileChanged(
                businessId, barberId, profile.bio(), profile.specialties(), profile.photoUrl()));
        return view(profile);
    }

    static ProfileView view(ProfessionalProfile profile) {
        return new ProfileView(profile.bio(), profile.specialties(), profile.photoUrl());
    }
}
