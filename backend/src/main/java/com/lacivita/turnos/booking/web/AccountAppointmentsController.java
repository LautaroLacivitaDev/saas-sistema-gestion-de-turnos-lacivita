package com.lacivita.turnos.booking.web;

import com.lacivita.turnos.booking.application.AccountHistory;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Mis turnos")
@RestController
class AccountAppointmentsController {

    private final AccountHistory history;

    AccountAppointmentsController(AccountHistory history) {
        this.history = history;
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

    record AccountAppointment(String businessName, String slug, BookingResponses.Appointment appointment) {}
}
