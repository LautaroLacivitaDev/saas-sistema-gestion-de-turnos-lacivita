package com.lacivita.turnos.booking.domain;

/**
 * Puerto: verifica que quien reserva como invitado es una persona y no un bot (Cloudflare Turnstile). La
 * implementación vive en la infraestructura.
 */
public interface HumanCheck {

    /**
     * @param token lo que devolvió el widget en el navegador
     * @param remoteIp IP de quien hace el pedido, para que el proveedor la compare
     */
    boolean isHuman(String token, String remoteIp);
}
