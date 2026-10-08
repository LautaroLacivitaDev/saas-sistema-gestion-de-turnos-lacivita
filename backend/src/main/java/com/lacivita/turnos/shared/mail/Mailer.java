package com.lacivita.turnos.shared.mail;

/**
 * Envío de emails transaccionales de la cuenta (verificación de email, link de acceso).
 *
 * <p>Los avisos de turnos no pasan por acá: los maneja el módulo de notificaciones, con plantillas,
 * outbox y reintentos.
 */
public interface Mailer {

    /**
     * Envía el email en segundo plano cuando la transacción actual se confirma. Si la transacción se
     * revierte, no se envía nada. Sin transacción activa, se envía enseguida (también en segundo plano).
     */
    void sendAfterCommit(MailMessage message);
}
