package com.lacivita.turnos.shared.security;

import com.lacivita.turnos.shared.tenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Fija en {@link TenantContext} a la persona con sesión iniciada durante el resto de la solicitud, para
 * que Row Level Security le muestre sus propias filas (por ejemplo, sus membresías en todos los
 * negocios).
 *
 * <p>Corre después de que la seguridad resolvió la sesión: es el único punto que traduce la seguridad al
 * contexto de datos.
 */
class SignedInUserScopeFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            chain.doFilter(request, response);
            return;
        }
        try {
            TenantContext.callAsUser(user.id(), () -> {
                chain.doFilter(request, response);
                return null;
            });
        } catch (IOException | ServletException | RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }
}
