package com.lacivita.turnos.booking.web;

import com.lacivita.turnos.booking.application.PublicBooking;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Reserva online desde la página pública. No exige sesión. */
@Tag(name = "Reserva online")
@RestController
@RequestMapping(PublicBookingController.BASE)
class PublicBookingController {

    static final String BASE = "/api/public/businesses/{slug}/holds";

    private final PublicBooking booking;

    PublicBookingController(PublicBooking booking) {
        this.booking = booking;
    }

    @Operation(
            summary = "Paso 1: reserva un horario unos minutos",
            description = "Para un servicio (serviceId) o un combo (comboId). Sin barberId se asigna \"cualquiera"
                    + " disponible\". El horario queda reservado 5 minutos (expiresAt). 409 slot_not_available si"
                    + " ya no está libre.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BookingResponses.Hold hold(@PathVariable String slug, @Valid @RequestBody BookingRequests.HoldData body) {
        return BookingResponses.Hold.from(
                booking.hold(slug, body.branchId(), body.item(), body.barberId(), body.startsAt()));
    }

    @Operation(
            summary = "Paso 2 (invitados): envía un código por email",
            description = "Guarda los datos del invitado y verifica con Cloudflare Turnstile (humanToken) que no"
                    + " sea un bot. Pedir otro código reemplaza el anterior.")
    @PostMapping("/{holdId}/guest-code")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void sendGuestCode(
            @PathVariable String slug,
            @PathVariable UUID holdId,
            @Valid @RequestBody BookingRequests.GuestData body,
            HttpServletRequest request) {
        booking.sendGuestCode(slug, holdId, body.contact(), body.humanToken(), request.getRemoteAddr());
    }

    @Operation(
            summary = "Paso 3: confirma el turno",
            description = "Con sesión y email verificado no hace falta código. Si no, va el código del email (5"
                    + " intentos). Llega por email el link para ver, cancelar o reprogramar el turno.")
    @PostMapping("/{holdId}/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    BookingResponses.Appointment confirm(
            @PathVariable String slug,
            @PathVariable UUID holdId,
            @Valid @RequestBody BookingRequests.ConfirmData body,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return BookingResponses.Appointment.from(booking.confirm(slug, holdId, body.code(), user));
    }
}
