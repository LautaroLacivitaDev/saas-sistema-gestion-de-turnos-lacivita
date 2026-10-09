package com.lacivita.turnos.booking.application;

import com.lacivita.turnos.booking.application.BookingViews.ManagedAppointmentView;
import com.lacivita.turnos.booking.domain.Appointment;
import com.lacivita.turnos.booking.domain.AppointmentLinkRepository;
import com.lacivita.turnos.booking.domain.AppointmentRepository;
import com.lacivita.turnos.booking.domain.BookingSettingsRepository;
import com.lacivita.turnos.booking.domain.ManageToken;
import com.lacivita.turnos.booking.domain.SlotNotAvailableException;
import com.lacivita.turnos.business.BusinessDirectory;
import com.lacivita.turnos.business.BusinessSummary;
import com.lacivita.turnos.schedule.FreeBarbers;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lo que el cliente hace con el link de su turno, sin iniciar sesión: verlo, confirmar que va, cancelarlo
 * o reprogramarlo, dentro del plazo del negocio.
 */
@Service
class CustomerChanges {

    private final AppointmentRepository appointments;
    private final AppointmentLinkRepository links;
    private final BookingSettingsRepository settings;
    private final AppointmentSlots slots;
    private final FreeBarbers freeBarbers;
    private final BusinessDirectory businesses;
    private final AppointmentViewer viewer;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    CustomerChanges(
            AppointmentRepository appointments,
            AppointmentLinkRepository links,
            BookingSettingsRepository settings,
            AppointmentSlots slots,
            FreeBarbers freeBarbers,
            BusinessDirectory businesses,
            AppointmentViewer viewer,
            ApplicationEventPublisher events,
            Clock clock) {
        this.appointments = appointments;
        this.links = links;
        this.settings = settings;
        this.slots = slots;
        this.freeBarbers = freeBarbers;
        this.businesses = businesses;
        this.viewer = viewer;
        this.events = events;
        this.clock = clock;
    }

    @BusinessScoped
    @Transactional(readOnly = true)
    public ManagedAppointmentView view(@BusinessId UUID businessId, ManageToken token) {
        return view(businessId, appointmentOf(token));
    }

    /** El cliente confirma que va a un turno que el local cargó "a confirmar". */
    @BusinessScoped
    @Transactional
    public ManagedAppointmentView confirm(@BusinessId UUID businessId, ManageToken token) {
        var appointment = appointmentOf(token);
        var before = appointment.getStatus();
        appointment.confirmByCustomer(clock.instant());
        events.publishEvent(BookingEvents.statusChanged(appointment, before));
        return view(businessId, appointment);
    }

    @BusinessScoped
    @Transactional
    public ManagedAppointmentView cancel(@BusinessId UUID businessId, ManageToken token) {
        var appointment = appointmentOf(token);
        var before = appointment.getStatus();
        appointment.cancelByCustomer(settings.cancellationOf(businessId), clock.instant());
        events.publishEvent(BookingEvents.statusChanged(appointment, before));
        return view(businessId, appointment);
    }

    /** Mueve el turno a otro horario libre del mismo profesional. */
    @BusinessScoped
    @Transactional
    public ManagedAppointmentView reschedule(@BusinessId UUID businessId, ManageToken token, Instant newStart) {
        var appointment = appointmentOf(token);
        var barberId = appointment.getBarberId();
        var free = freeBarbers.at(
                businessId, appointment.getBranchId(), appointment.item(), barberId, newStart, appointment.getId());
        if (!free.contains(barberId)) {
            throw new SlotNotAvailableException();
        }
        var now = clock.instant();
        var startBefore = appointment.getStartsAt();
        slots.releaseExpiredHolds(barberId, now);
        appointment.rescheduleByCustomer(settings.cancellationOf(businessId), newStart, now);
        slots.flushMove();
        events.publishEvent(BookingEvents.rescheduled(appointment, barberId, startBefore));
        return view(businessId, appointment);
    }

    private Appointment appointmentOf(ManageToken token) {
        return appointments.require(links.requireAppointmentId(token));
    }

    private ManagedAppointmentView view(UUID businessId, Appointment appointment) {
        var notice = Duration.ofHours(settings.cancellationOf(businessId).noticeHours());
        return new ManagedAppointmentView(
                businesses.find(businessId).map(BusinessSummary::name).orElse(""),
                viewer.view(businessId, appointment),
                appointment.getStartsAt().minus(notice));
    }
}
