package com.lacivita.turnos.business.web;

import com.lacivita.turnos.shared.security.PublicEndpoint;
import com.lacivita.turnos.shared.security.PublicEndpoints;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/** La página pública de cada negocio no exige sesión. */
@Component
class BusinessPublicEndpoints implements PublicEndpoints {

    @Override
    public List<PublicEndpoint> publicEndpoints() {
        return List.of(PublicEndpoint.open(HttpMethod.GET, PublicBusinessController.BASE + "/*"));
    }
}
