package com.lacivita.turnos.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/** Inicia la sesión de una persona ya autenticada, sea cual sea el método que usó. */
@Component
public class SessionAuthenticator {

    private final SecurityContextRepository contextRepository;
    private final SecurityContextHolderStrategy holder = SecurityContextHolder.getContextHolderStrategy();

    SessionAuthenticator(SecurityContextRepository contextRepository) {
        this.contextRepository = contextRepository;
    }

    public void signIn(AuthenticatedUser user, HttpServletRequest request, HttpServletResponse response) {
        // Un id de sesión nuevo al iniciar sesión evita la fijación de sesión.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        var authentication = UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities());
        var context = holder.createEmptyContext();
        context.setAuthentication(authentication);
        holder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }
}
