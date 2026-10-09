package com.lacivita.turnos.schedule.application;

import com.lacivita.turnos.schedule.application.ScheduleViews.RulesView;
import com.lacivita.turnos.schedule.domain.ScheduleRules;
import com.lacivita.turnos.schedule.domain.ScheduleSettings;
import com.lacivita.turnos.schedule.domain.ScheduleSettingsRepository;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reglas de agenda del negocio: tiempo de preparación, anticipación e intervalo entre horarios. */
@Service
public class ScheduleSettingsService {

    private final ScheduleSettingsRepository settings;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    ScheduleSettingsService(ScheduleSettingsRepository settings, ApplicationEventPublisher events, Clock clock) {
        this.settings = settings;
        this.events = events;
        this.clock = clock;
    }

    @BusinessScoped
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public RulesView rules(@BusinessId UUID businessId) {
        return toView(settings.rulesOf(businessId));
    }

    /** Solo el dueño: son políticas del negocio. */
    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'OWNER')")
    public RulesView change(@BusinessId UUID businessId, ScheduleRules rules) {
        var now = clock.instant();
        var before = settings.rulesOf(businessId);
        settings.findById(businessId)
                .ifPresentOrElse(
                        existing -> existing.change(rules, now),
                        () -> settings.save(ScheduleSettings.of(businessId, rules, now)));
        events.publishEvent(new ScheduleEvents.RulesChanged(businessId, before, rules));
        return toView(rules);
    }

    private static RulesView toView(ScheduleRules rules) {
        return new RulesView(
                rules.bufferMinutes(), rules.minNoticeMinutes(), rules.maxAdvanceDays(), rules.slotStepMinutes());
    }
}
