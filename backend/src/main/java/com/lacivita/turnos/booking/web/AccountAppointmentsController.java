package com.lacivita.turnos.booking.web;

import com.lacivita.turnos.booking.application.AccountHistory;
import com.lacivita.turnos.booking.application.AccountManageLinks;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Mis turnos")
@RestController
class AccountAppointmentsController {

    private final AccountHistory history;
    private final AccountManageLinks manageLinks;

    AccountAppointmentsController(AccountHistory history, AccountManageLinks manageLinks) {
        this.history = history;
        this.manageLinks = manageLinks;
    }

    @Operation(
            summary = "Mis turnos como cliente",
            description = "Los últimos 50, en todos los negocios donde reservé con mi cuenta, del más nuevo al más"
                    + " viejo. Con el negocio y lo reservado, para repetir una reserva.")
    @GetMapping("/api/me/appointments")
    List<AccountAppointment> mine(@AuthenticationPrincipal AuthenticatedUser user) {
        return history.of(user).stream()
                .map(item -> new AccountAppointment(
                        item.businessName(), item.slug(), BookingResponses.Appointment.from(item.appointment())))
                .toList();
    }

    @Operation(
            summary = "Abre la gestión de uno de mis turnos",
            description = "Devuelve un link de gestión nuevo (el mismo que llega por email) para cambiarlo o"
                    + " cancelarlo. 404 si el turno no es de la cuenta.")
    @PostMapping("/api/me/appointments/{appointmentId}/manage-link")
    ManageLink manageLink(@PathVariable UUID appointmentId, @AuthenticationPrincipal AuthenticatedUser user) {
        return new ManageLink(manageLinks.issueFor(user, appointmentId).value());
    }

    /** @param token secreto para {@code /turno?token=}; no se guarda: es la única vez que se muestra */
    record ManageLink(String token) {}

    record AccountAppointment(String businessName, String slug, BookingResponses.Appointment appointment) {}
}
