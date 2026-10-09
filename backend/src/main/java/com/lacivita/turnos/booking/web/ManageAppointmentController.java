package com.lacivita.turnos.booking.web;

import com.lacivita.turnos.booking.application.ManageLinks;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lo que el cliente hace con el link de su turno, sin iniciar sesión. El token viaja en el cuerpo y no en
 * la URL, para que no quede en logs ni en el historial.
 */
@Tag(name = "Turno del cliente")
@RestController
@RequestMapping(ManageAppointmentController.BASE)
class ManageAppointmentController {

    static final String BASE = "/api/public/appointments";

    private final ManageLinks links;

    ManageAppointmentController(ManageLinks links) {
        this.links = links;
    }

    @Operation(summary = "Muestra el turno del link", description = "changeableUntil: hasta cuándo se puede cambiar.")
    @PostMapping("/lookup")
    BookingResponses.ManagedAppointment view(@Valid @RequestBody BookingRequests.TokenData body) {
        return BookingResponses.ManagedAppointment.from(links.view(body.token()));
    }

    @Operation(
            summary = "Confirma que el cliente va",
            description = "Para turnos que el local cargó a confirmar, antes de su hora.")
    @PostMapping("/confirm")
    BookingResponses.ManagedAppointment confirm(@Valid @RequestBody BookingRequests.TokenData body) {
        return BookingResponses.ManagedAppointment.from(links.confirm(body.token()));
    }

    @Operation(summary = "Cancela el turno", description = "Dentro del plazo del negocio (422 change_deadline_passed).")
    @PostMapping("/cancel")
    BookingResponses.ManagedAppointment cancel(@Valid @RequestBody BookingRequests.TokenData body) {
        return BookingResponses.ManagedAppointment.from(links.cancel(body.token()));
    }

    @Operation(
            summary = "Reprograma el turno",
            description = "A otro horario libre del mismo profesional, dentro del plazo del negocio.")
    @PostMapping("/reschedule")
    BookingResponses.ManagedAppointment reschedule(@Valid @RequestBody BookingRequests.CustomerRescheduleData body) {
        return BookingResponses.ManagedAppointment.from(links.reschedule(body.token(), body.startsAt()));
    }
}
