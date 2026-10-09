package com.lacivita.turnos.booking.web;

import com.lacivita.turnos.booking.application.BookingViews.AppointmentView;
import com.lacivita.turnos.booking.application.BookingViews.CustomerView;
import com.lacivita.turnos.booking.application.BookingViews.HoldView;
import com.lacivita.turnos.booking.application.BookingViews.LineView;
import com.lacivita.turnos.booking.application.BookingViews.ManagedAppointmentView;
import com.lacivita.turnos.booking.application.BookingViews.SettingsView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Respuestas HTTP de las reservas: son el contrato de la API. */
final class BookingResponses {

    private BookingResponses() {}

    record Line(UUID serviceId, String serviceName, BigDecimal price, int durationMinutes) {

        static Line from(LineView view) {
            return new Line(view.serviceId(), view.serviceName(), view.price(), view.durationMinutes());
        }

        static List<Line> from(List<LineView> views) {
            return views.stream().map(Line::from).toList();
        }
    }

    record Customer(UUID id, String name, String email, String phone) {

        static Customer from(CustomerView view) {
            return view == null ? null : new Customer(view.id(), view.name(), view.email(), view.phone());
        }
    }

    record Hold(
            UUID holdId,
            Instant expiresAt,
            UUID branchId,
            UUID barberId,
            String barberName,
            Instant startsAt,
            Instant endsAt,
            BigDecimal totalPrice,
            List<Line> lines) {

        static Hold from(HoldView view) {
            return new Hold(
                    view.holdId(),
                    view.expiresAt(),
                    view.branchId(),
                    view.barberId(),
                    view.barberName(),
                    view.startsAt(),
                    view.endsAt(),
                    view.totalPrice(),
                    Line.from(view.lines()));
        }
    }

    /** @param customer datos del cliente; nulos si quien consulta no los puede ver */
    record Appointment(
            UUID id,
            UUID branchId,
            String branchName,
            String timeZone,
            UUID barberId,
            String barberName,
            String status,
            String source,
            Instant startsAt,
            Instant endsAt,
            BigDecimal totalPrice,
            List<Line> lines,
            UUID comboId,
            Customer customer) {

        static Appointment from(AppointmentView view) {
            return new Appointment(
                    view.id(),
                    view.branchId(),
                    view.branchName(),
                    view.timeZone(),
                    view.barberId(),
                    view.barberName(),
                    view.status(),
                    view.source(),
                    view.startsAt(),
                    view.endsAt(),
                    view.totalPrice(),
                    Line.from(view.lines()),
                    view.comboId(),
                    Customer.from(view.customer()));
        }
    }

    record ManagedAppointment(String businessName, Appointment appointment, Instant changeableUntil) {

        static ManagedAppointment from(ManagedAppointmentView view) {
            return new ManagedAppointment(
                    view.businessName(), Appointment.from(view.appointment()), view.changeableUntil());
        }
    }

    record Settings(int cancellationNoticeHours) {

        static Settings from(SettingsView view) {
            return new Settings(view.cancellationNoticeHours());
        }
    }
}
