package com.lacivita.turnos.booking.application;

import com.lacivita.turnos.booking.application.BookingViews.SettingsView;
import com.lacivita.turnos.booking.domain.BookingSettings;
import com.lacivita.turnos.booking.domain.BookingSettingsRepository;
import com.lacivita.turnos.booking.domain.CancellationPolicy;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Políticas de reserva del negocio: por ahora, el plazo para que el cliente cancele o reprograme. */
@Service
public class BookingSettingsService {

    private final BookingSettingsRepository settings;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    BookingSettingsService(BookingSettingsRepository settings, ApplicationEventPublisher events, Clock clock) {
        this.settings = settings;
        this.events = events;
        this.clock = clock;
    }

    @BusinessScoped
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public SettingsView of(@BusinessId UUID businessId) {
        return new SettingsView(settings.cancellationOf(businessId).noticeHours());
    }

    /** Solo el dueño: es una política del negocio. */
    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'OWNER')")
    public SettingsView change(@BusinessId UUID businessId, CancellationPolicy policy) {
        var now = clock.instant();
        int before = settings.cancellationOf(businessId).noticeHours();
        settings.findById(businessId)
                .ifPresentOrElse(
                        existing -> existing.change(policy, now),
                        () -> settings.save(BookingSettings.of(businessId, policy, now)));
        events.publishEvent(new BookingEvents.CancellationPolicyChanged(businessId, before, policy.noticeHours()));
        return new SettingsView(policy.noticeHours());
    }
}
