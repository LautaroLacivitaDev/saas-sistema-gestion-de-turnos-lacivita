package com.lacivita.turnos.catalog.web;

import com.lacivita.turnos.shared.security.PublicEndpoint;
import com.lacivita.turnos.shared.security.PublicEndpoints;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/** El catálogo de la página pública no exige sesión. */
@Component
class CatalogPublicEndpoints implements PublicEndpoints {

    @Override
    public List<PublicEndpoint> publicEndpoints() {
        return List.of(PublicEndpoint.open(HttpMethod.GET, "/api/public/businesses/*/catalog"));
    }
}
