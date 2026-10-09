package com.lacivita.turnos.business.domain;

import com.lacivita.turnos.shared.domain.InvalidValueException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Nombre del negocio en su link público ({@code dominio.com/<slug>}). Solo minúsculas, números y
 * guiones simples, sin guion al principio ni al final.
 *
 * <p>Las palabras reservadas son rutas de la aplicación: un negocio que las use taparía una página.
 */
public record Slug(String value) {

    static final int MIN_LENGTH = 3;
    static final int MAX_LENGTH = 50;

    private static final Pattern FORMAT = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    static final Set<String> RESERVED = Set.of(
            "acceso",
            "actuator",
            "admin",
            "api",
            "app",
            "assets",
            "ayuda",
            "blog",
            "buscar",
            "busqueda",
            "configuracion",
            "contacto",
            "cuenta",
            "docs",
            "ingresar",
            "invitacion",
            "invitaciones",
            "login",
            "logout",
            "mail",
            "mis-turnos",
            "negocios",
            "nosotros",
            "panel",
            "precios",
            "privacidad",
            "public",
            "registro",
            "registrarse",
            "reservas",
            "soporte",
            "static",
            "sucursales",
            "terminos",
            "turno",
            "turnos",
            "verificar-email",
            "www");

    public Slug {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException("invalid_slug", "Elegí el nombre para tu link.");
        }
        value = value.strip().toLowerCase(Locale.ROOT);
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new InvalidValueException(
                    "invalid_slug",
                    "El link tiene que tener entre " + MIN_LENGTH + " y " + MAX_LENGTH + " caracteres.");
        }
        if (!FORMAT.matcher(value).matches()) {
            throw new InvalidValueException(
                    "invalid_slug", "El link solo puede tener letras minúsculas sin tildes, números y guiones.");
        }
        if (RESERVED.contains(value)) {
            throw new InvalidValueException("reserved_slug", "Ese nombre está reservado. Probá con otro.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
