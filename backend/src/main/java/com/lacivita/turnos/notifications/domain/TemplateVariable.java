package com.lacivita.turnos.notifications.domain;

import java.util.Arrays;
import java.util.Optional;

/** Datos del turno que el negocio puede usar en sus textos, escritos entre llaves: {@code {nombre}}. */
public enum TemplateVariable {
    NOMBRE("nombre", "Nombre del cliente"),
    SERVICIO("servicio", "Servicios del turno"),
    BARBERO("barbero", "Profesional que atiende"),
    SUCURSAL("sucursal", "Nombre de la sucursal"),
    DIRECCION("direccion", "Dirección de la sucursal"),
    HORA("hora", "Día y hora del turno"),
    NEGOCIO("negocio", "Nombre del negocio");

    private final String key;
    private final String description;

    TemplateVariable(String key, String description) {
        this.key = key;
        this.description = description;
    }

    public static Optional<TemplateVariable> byKey(String key) {
        return Arrays.stream(values())
                .filter(variable -> variable.key.equals(key))
                .findFirst();
    }

    public String key() {
        return key;
    }

    public String description() {
        return description;
    }

    /** Cómo se escribe en el texto, por ejemplo {@code {nombre}}. */
    public String placeholder() {
        return "{" + key + "}";
    }
}
