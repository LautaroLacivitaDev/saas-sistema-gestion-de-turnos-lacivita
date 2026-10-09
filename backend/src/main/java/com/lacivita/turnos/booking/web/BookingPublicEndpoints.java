package com.lacivita.turnos.booking.web;

import com.lacivita.turnos.shared.security.PublicEndpoint;
import com.lacivita.turnos.shared.security.PublicEndpoints;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/**
 * La reserva online y el link del cliente no exigen sesión. Tomar horarios, pedir códigos y confirmar
 * llevan límite de intentos: frenan a quien quiera llenar la agenda, enviar emails masivos o adivinar
 * códigos.
 */
@Component
class BookingPublicEndpoints implements PublicEndpoints {

    private static final String HOLDS = "/api/public/businesses/*/holds";

    @Override
    public List<PublicEndpoint> publicEndpoints() {
        return List.of(
                PublicEndpoint.rateLimited(HttpMethod.POST, HOLDS),
                PublicEndpoint.rateLimited(HttpMethod.POST, HOLDS + "/*/guest-code"),
                PublicEndpoint.rateLimited(HttpMethod.POST, HOLDS + "/*/confirm"),
                PublicEndpoint.open(HttpMethod.POST, ManageAppointmentController.BASE + "/*"));
    }
}
