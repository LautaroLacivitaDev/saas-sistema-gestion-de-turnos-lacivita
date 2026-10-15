package com.lacivita.turnos.booking.application;

import com.lacivita.turnos.booking.domain.AppointmentNotFoundException;
import com.lacivita.turnos.booking.domain.AppointmentRepository;
import com.lacivita.turnos.booking.domain.ManageToken;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.tenancy.TenantContext;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Desde "Mis turnos", quien reservó con su cuenta abre la gestión de su turno (cambiarlo o cancelarlo) sin
 * buscar el email: se le emite un link de gestión nuevo, con las mismas reglas que el del email.
 *
 * <p>DECISIÓN: se reutiliza la gestión por link en lugar de duplicar confirmar, cambiar y cancelar para la
 * cuenta. La cuenta solo prueba que el turno es suyo; desde ahí rigen las reglas de siempre (plazo, estado).
 */
@Service
public class AccountManageLinks {

    private final AppointmentRepository appointments;
    private final ManageLinkIssuer links;

    AccountManageLinks(AppointmentRepository appointments, ManageLinkIssuer links) {
        this.appointments = appointments;
        this.links = links;
    }

    /**
     * @throws AppointmentNotFoundException si el turno no existe o no lo reservó esa cuenta (no se distingue,
     *     para no revelar turnos ajenos)
     */
    public ManageToken issueFor(AuthenticatedUser user, UUID appointmentId) {
        var found = TenantContext.callAsSystem(
                "mis turnos: el negocio de un turno de la cuenta",
                () -> appointments.findForAccount(user.id(), appointmentId));
        var appointment = found.orElseThrow(AppointmentNotFoundException::new);
        return links.issue(appointment.getBusinessId(), appointment.getAppointmentId());
    }
}
