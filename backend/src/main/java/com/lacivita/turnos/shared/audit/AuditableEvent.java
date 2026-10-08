package com.lacivita.turnos.shared.audit;

import java.util.Optional;
import java.util.UUID;

/**
 * Evento de dominio que deja rastro en el registro de auditoría. Los módulos publican sus eventos y
 * {@link AuditTrail} los guarda en la misma transacción que el cambio: si el cambio se revierte, el
 * registro también.
 *
 * <p>Quién hizo el cambio y cuándo lo completa el auditor; el evento describe qué cambió.
 */
public interface AuditableEvent {

    /** Negocio al que pertenece el cambio. */
    Optional<UUID> auditBusinessId();

    /** Acción en formato {@code entidad.verbo}, por ejemplo {@code business.slug_changed}. */
    String auditAction();

    String auditEntityType();

    String auditEntityId();

    /** Estado anterior. Se guarda como JSON. */
    default Optional<Object> auditBefore() {
        return Optional.empty();
    }

    /** Estado nuevo. Se guarda como JSON. */
    default Optional<Object> auditAfter() {
        return Optional.empty();
    }
}
