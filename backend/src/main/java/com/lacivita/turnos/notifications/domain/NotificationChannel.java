package com.lacivita.turnos.notifications.domain;

/**
 * Canal por el que salen las notificaciones. La lógica de qué avisar, a quién y cuándo no depende del
 * canal: sumar uno (WhatsApp, por ejemplo) es implementar esta interfaz.
 */
public interface NotificationChannel {

    ChannelKind kind();

    /**
     * Envía el mensaje.
     *
     * @throws DeliveryFailedException si no salió; se reintenta más tarde
     */
    void send(OutgoingMessage message);
}
