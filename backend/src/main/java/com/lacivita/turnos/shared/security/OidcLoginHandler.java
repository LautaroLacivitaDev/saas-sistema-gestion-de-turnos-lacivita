package com.lacivita.turnos.shared.security;

import com.lacivita.turnos.shared.config.AppProperties;
import com.lacivita.turnos.shared.domain.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * Cierra el login con Google (OpenID Connect): convierte la identidad de Google en una cuenta propia,
 * inicia la sesión con el mismo principal que el resto de los métodos y vuelve al frontend.
 */
class OidcLoginHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OidcLoginHandler.class);

    private final ExternalIdentityResolver identities;
    private final SessionAuthenticator sessions;
    private final AppProperties properties;

    OidcLoginHandler(ExternalIdentityResolver identities, SessionAuthenticator sessions, AppProperties properties) {
        this.identities = identities;
        this.sessions = sessions;
        this.properties = properties;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        if (!(authentication instanceof OAuth2AuthenticationToken token
                && token.getPrincipal() instanceof OidcUser oidcUser)) {
            redirectToFailure(response, "unsupported_provider");
            return;
        }
        var identity = new ExternalIdentity(
                token.getAuthorizedClientRegistrationId().toUpperCase(Locale.ROOT),
                oidcUser.getSubject(),
                oidcUser.getEmail(),
                Boolean.TRUE.equals(oidcUser.getEmailVerified()),
                oidcUser.getFullName());
        try {
            sessions.signIn(identities.resolve(identity), request, response);
            response.sendRedirect(properties.frontendLink("/"));
        } catch (DomainException ex) {
            request.getSession().invalidate();
            redirectToFailure(response, ex.code());
        }
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        log.warn("Falló el login con proveedor externo: {}", exception.getMessage());
        redirectToFailure(response, "external_login_failed");
    }

    private void redirectToFailure(HttpServletResponse response, String code) throws IOException {
        response.sendRedirect(properties.frontendLink("/ingresar?error=" + code));
    }
}
