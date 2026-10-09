package com.lacivita.turnos.notifications.domain;

/**
 * Un canal no pudo enviar un mensaje. No es un error para el usuario: queda en el registro de envíos y se
 * reintenta.
 *
 * @see Notification#markFailed(String, java.time.Instant)
 */
public class DeliveryFailedException extends RuntimeException {

    private final String reason;

    /** @param reason código técnico y breve, sin datos personales (por ejemplo {@code smtp_error}) */
    public DeliveryFailedException(String reason, Throwable cause) {
        super(reason, cause);
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}
