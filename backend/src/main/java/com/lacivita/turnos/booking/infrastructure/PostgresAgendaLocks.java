package com.lacivita.turnos.booking.infrastructure;

import com.lacivita.turnos.booking.domain.AgendaLocks;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Bloqueo consultivo de PostgreSQL por profesional, atado a la transacción (se suelta al confirmar o al
 * revertir).
 *
 * <p>Va por el {@link EntityManager} y no por {@code JdbcClient}: tiene que correr en la misma conexión que
 * la transacción de JPA (la que entrega el proveedor de conexiones por negocio). Por otra conexión, el
 * bloqueo no protegería nada y cada pedido ocuparía dos conexiones a la vez.
 */
@Component
class PostgresAgendaLocks implements AgendaLocks {

    private final EntityManager entityManager;

    PostgresAgendaLocks(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void lockAgendaOf(UUID barberId) {
        // pg_advisory_xact_lock no devuelve valor: se envuelve para leer un entero.
        entityManager
                .createNativeQuery("SELECT 1 FROM (SELECT pg_advisory_xact_lock(:key)) AS agenda_lock")
                .setParameter("key", keyOf(barberId))
                .getSingleResult();
    }

    /** Los 128 bits del UUID en los 64 de la clave: un choque solo haría esperar de más, nunca fallar. */
    private static long keyOf(UUID barberId) {
        return barberId.getMostSignificantBits() ^ barberId.getLeastSignificantBits();
    }
}
