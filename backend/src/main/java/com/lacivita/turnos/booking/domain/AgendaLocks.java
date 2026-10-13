package com.lacivita.turnos.booking.domain;

import java.util.UUID;

/**
 * Turno para escribir en la agenda de un profesional. Quien lo toma espera a que termine la transacción que
 * lo tenía y lo suelta al terminar la suya.
 *
 * <p>La restricción de exclusión de la base sigue siendo la que impide la doble reserva. Este turno evita
 * que, con muchas personas intentando el mismo horario a la vez, PostgreSQL detecte interbloqueos entre
 * ellas (cada una espera a las otras al chequear la restricción): así la primera gana y las demás reciben
 * enseguida "ese horario ya no está".
 */
public interface AgendaLocks {

    void lockAgendaOf(UUID barberId);
}
