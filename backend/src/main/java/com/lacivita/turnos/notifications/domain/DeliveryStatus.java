package com.lacivita.turnos.notifications.domain;

/** Estado del envío de una notificación, tal como se muestra en la ficha del turno. */
// DECISIÓN: no hay estado "entregado". Por SMTP solo se sabe que el proveedor aceptó el email; saber si
// llegó a la casilla requiere los webhooks del proveedor transaccional (Resend o SES), que se suman cuando
// se elija uno para producción.
public enum DeliveryStatus {
    /** Espera su hora o un nuevo intento. */
    PENDING,
    /** El proveedor de email lo aceptó. */
    SENT,
    /** Se agotaron los intentos. */
    FAILED,
    /** Ya no corresponde: por ejemplo, el recordatorio de un turno que se movió o se canceló. */
    CANCELLED,
    /** No se pudo enviar por un motivo que no se arregla reintentando (por ejemplo, el cliente no dejó email). */
    SKIPPED
}
