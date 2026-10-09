package com.lacivita.turnos.notifications.application;

import com.lacivita.turnos.notifications.application.NotificationViews.SettingsView;
import com.lacivita.turnos.notifications.domain.NotificationSettings;
import com.lacivita.turnos.notifications.domain.NotificationSettingsRepository;
import com.lacivita.turnos.notifications.domain.ReminderSchedule;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuándo se le recuerda el turno al cliente. Un cambio rige para los turnos que se reserven o se muevan
 * desde ese momento; los recordatorios ya programados no cambian.
 */
@Service
public class NotificationSettingsService {

    private final NotificationSettingsRepository settings;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    NotificationSettingsService(
            NotificationSettingsRepository settings, ApplicationEventPublisher events, Clock clock) {
        this.settings = settings;
        this.events = events;
        this.clock = clock;
    }

    @BusinessScoped
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public SettingsView of(@BusinessId UUID businessId) {
        return new SettingsView(settings.remindersOf(businessId).hoursBefore());
    }

    /** Solo el dueño: es una política del negocio. */
    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'OWNER')")
    public SettingsView change(@BusinessId UUID businessId, ReminderSchedule reminders) {
        var now = clock.instant();
        var before = settings.remindersOf(businessId);
        settings.findById(businessId)
                .ifPresentOrElse(
                        existing -> existing.change(reminders, now),
                        () -> settings.save(NotificationSettings.of(businessId, reminders, now)));
        events.publishEvent(
                new NotificationEvents.RemindersChanged(businessId, before.hoursBefore(), reminders.hoursBefore()));
        return new SettingsView(reminders.hoursBefore());
    }
}
