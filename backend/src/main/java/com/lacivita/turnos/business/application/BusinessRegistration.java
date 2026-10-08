package com.lacivita.turnos.business.application;

import com.lacivita.turnos.business.BusinessRegistered;
import com.lacivita.turnos.business.application.BusinessViews.BusinessView;
import com.lacivita.turnos.business.domain.Business;
import com.lacivita.turnos.business.domain.BusinessProfile;
import com.lacivita.turnos.business.domain.Slug;
import com.lacivita.turnos.business.domain.SlugTakenException;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Alta de negocios. Cualquier persona con sesión puede crear uno y queda como dueña. */
@Service
public class BusinessRegistration {

    private final SlugGuard slugs;
    private final ApplicationEventPublisher events;
    private final BusinessMapper mapper;
    private final Clock clock;

    BusinessRegistration(SlugGuard slugs, ApplicationEventPublisher events, BusinessMapper mapper, Clock clock) {
        this.slugs = slugs;
        this.events = events;
        this.mapper = mapper;
        this.clock = clock;
    }

    /**
     * @param newBusinessId id del negocio nuevo; lo genera quien llama porque es el contexto de
     *     aislamiento con el que se guarda todo el alta
     */
    @BusinessScoped
    @Transactional
    public BusinessView register(
            @BusinessId UUID newBusinessId, AuthenticatedUser owner, BusinessProfile profile, Slug slug) {
        if (slugs.isTaken(slug.value())) {
            throw new SlugTakenException();
        }
        var business = Business.register(newBusinessId, profile, slug, clock.instant());
        slugs.saveNew(business);
        events.publishEvent(new BusinessRegistered(business.getId(), owner.id(), profile.name(), slug.value()));
        return mapper.toView(business);
    }
}
