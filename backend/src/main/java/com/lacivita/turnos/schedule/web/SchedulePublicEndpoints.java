package com.lacivita.turnos.schedule.web;

import com.lacivita.turnos.shared.security.PublicEndpoint;
import com.lacivita.turnos.shared.security.PublicEndpoints;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/** Los horarios de atención y la disponibilidad de la página pública no exigen sesión. */
@Component
class SchedulePublicEndpoints implements PublicEndpoints {

    @Override
    public List<PublicEndpoint> publicEndpoints() {
        return List.of(
                PublicEndpoint.open(HttpMethod.GET, "/api/public/businesses/*/branches/*/hours"),
                PublicEndpoint.open(HttpMethod.GET, "/api/public/businesses/*/availability"));
    }
}
