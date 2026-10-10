package com.lacivita.turnos.notifications.web;

import com.lacivita.turnos.shared.security.PublicEndpoint;
import com.lacivita.turnos.shared.security.PublicEndpoints;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/** El buzón de la demo se ve sin sesión, con límite de intentos para que no se pueda recorrer. */
@Component
@ConditionalOnBooleanProperty("app.demo.enabled")
class DemoPublicEndpoints implements PublicEndpoints {

    @Override
    public List<PublicEndpoint> publicEndpoints() {
        return List.of(
                PublicEndpoint.rateLimited(HttpMethod.GET, "/api/public/demo/inbox"),
                PublicEndpoint.rateLimited(HttpMethod.GET, "/api/public/demo/inbox/*/turno.ics"));
    }
}
