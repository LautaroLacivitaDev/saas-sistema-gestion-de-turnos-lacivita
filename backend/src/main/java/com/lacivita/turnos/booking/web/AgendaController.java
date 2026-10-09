package com.lacivita.turnos.booking.web;

import com.lacivita.turnos.booking.application.Agenda;
import com.lacivita.turnos.booking.application.BookingSettingsService;
import com.lacivita.turnos.shared.domain.TimeInterval;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Agenda de turnos")
@RestController
@RequestMapping("/api/businesses/{businessId}")
class AgendaController {

    private final Agenda agenda;
    private final BookingSettingsService settings;

    AgendaController(Agenda agenda, BookingSettingsService settings) {
        this.agenda = agenda;
        this.settings = settings;
    }

    @Operation(
            summary = "Turnos del período",
            description = "De las sucursales de quien consulta (hasta un mes). El contacto del cliente lo ven el"
                    + " dueño, los gerentes y el profesional del turno.")
    @GetMapping("/appointments")
    List<BookingResponses.Appointment> appointments(
            @PathVariable UUID businessId,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) UUID barberId,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return agenda.of(businessId, actor, new TimeInterval(from, to), branchId, barberId).stream()
                .map(BookingResponses.Appointment::from)
                .toList();
    }

    @Operation(
            summary = "Carga un turno",
            description = "Para clientes que llaman o llegan sin reserva. No se puede superponer con otro turno"
                    + " del profesional (409 slot_not_available).")
    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    BookingResponses.Appointment book(
            @PathVariable UUID businessId,
            @Valid @RequestBody BookingRequests.CounterData body,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        var booking = new Agenda.CounterBooking(
                body.branchId(),
                body.barberId(),
                body.item(),
                body.startsAt(),
                body.customerId(),
                body.contact(),
                body.isConfirmed());
        return BookingResponses.Appointment.from(agenda.book(businessId, actor, booking));
    }

    @Operation(
            summary = "Cambia el estado de un turno",
            description = "CONFIRMED, IN_PROGRESS, COMPLETED, NO_SHOW o CANCELLED, según el estado actual.")
    @PutMapping("/appointments/{appointmentId}/status")
    BookingResponses.Appointment changeStatus(
            @PathVariable UUID businessId,
            @PathVariable UUID appointmentId,
            @Valid @RequestBody BookingRequests.StatusData body,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return BookingResponses.Appointment.from(agenda.changeStatus(businessId, actor, appointmentId, body.status()));
    }

    @Operation(summary = "Mueve un turno", description = "A otro horario y, si se indica, a otro profesional.")
    @PutMapping("/appointments/{appointmentId}/time")
    BookingResponses.Appointment reschedule(
            @PathVariable UUID businessId,
            @PathVariable UUID appointmentId,
            @Valid @RequestBody BookingRequests.StaffRescheduleData body,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return BookingResponses.Appointment.from(
                agenda.reschedule(businessId, actor, appointmentId, body.startsAt(), body.barberId()));
    }

    @Operation(summary = "Políticas de reserva", description = "Todo el equipo.")
    @GetMapping("/booking-settings")
    BookingResponses.Settings settings(@PathVariable UUID businessId) {
        return BookingResponses.Settings.from(settings.of(businessId));
    }

    @Operation(
            summary = "Cambia el plazo para que el cliente cancele o reprograme",
            description = "Solo el dueño. En horas antes del turno (0 a 168).")
    @PutMapping("/booking-settings")
    BookingResponses.Settings changeSettings(
            @PathVariable UUID businessId, @Valid @RequestBody BookingRequests.SettingsData body) {
        return BookingResponses.Settings.from(settings.change(businessId, body.policy()));
    }
}
