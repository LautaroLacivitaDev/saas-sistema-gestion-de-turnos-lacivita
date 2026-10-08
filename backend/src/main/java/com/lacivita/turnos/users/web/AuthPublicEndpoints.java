package com.lacivita.turnos.users.web;

import static com.lacivita.turnos.users.web.AuthPaths.full;

import com.lacivita.turnos.shared.security.PublicEndpoint;
import com.lacivita.turnos.shared.security.PublicEndpoints;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/**
 * Endpoints de autenticación que no exigen sesión. Los que crean cuentas, prueban credenciales o
 * envían emails llevan límite de intentos.
 */
@Component
class AuthPublicEndpoints implements PublicEndpoints {

    @Override
    public List<PublicEndpoint> publicEndpoints() {
        return List.of(
                PublicEndpoint.open(HttpMethod.GET, full(AuthPaths.CSRF)),
                PublicEndpoint.rateLimited(HttpMethod.POST, full(AuthPaths.REGISTER)),
                PublicEndpoint.rateLimited(HttpMethod.POST, full(AuthPaths.LOGIN)),
                PublicEndpoint.rateLimited(HttpMethod.POST, full(AuthPaths.LOGIN_LINK)),
                PublicEndpoint.rateLimited(HttpMethod.POST, full(AuthPaths.LOGIN_LINK_CONSUME)),
                PublicEndpoint.rateLimited(HttpMethod.POST, full(AuthPaths.EMAIL_VERIFICATION)));
    }
}
