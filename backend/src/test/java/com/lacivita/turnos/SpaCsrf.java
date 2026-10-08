package com.lacivita.turnos;

import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Manda el token CSRF como lo hace el frontend: el mismo valor en la cookie {@code XSRF-TOKEN} y en el
 * encabezado {@code X-XSRF-TOKEN}.
 *
 * <p>Se usa en lugar de {@code SecurityMockMvcRequestPostProcessors.csrf()}, que reemplaza el repositorio
 * CSRF del filtro real dentro del contexto compartido y deja a las pruebas siguientes con otra
 * configuración (por ejemplo, otro nombre de encabezado).
 */
public final class SpaCsrf {

    private SpaCsrf() {}

    public static RequestPostProcessor spaCsrf() {
        return request -> {
            String token = UUID.randomUUID().toString();
            var cookies = new ArrayList<Cookie>();
            if (request.getCookies() != null) {
                cookies.addAll(Arrays.asList(request.getCookies()));
            }
            cookies.add(new Cookie("XSRF-TOKEN", token));
            request.setCookies(cookies.toArray(Cookie[]::new));
            request.addHeader("X-XSRF-TOKEN", token);
            return request;
        };
    }
}
