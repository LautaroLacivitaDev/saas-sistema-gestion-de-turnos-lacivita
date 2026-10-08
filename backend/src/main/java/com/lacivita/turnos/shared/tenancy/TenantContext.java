package com.lacivita.turnos.shared.tenancy;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Quién opera y sobre qué negocio en el hilo actual. Lo leen el filtro de Hibernate y la conexión a
 * PostgreSQL (Row Level Security).
 *
 * <p>El alcance se fija <strong>antes</strong> de abrir la transacción: la conexión toma el contexto al
 * obtenerse. Por eso los casos de uso lo declaran con {@link BusinessScoped} en lugar de fijarlo adentro.
 *
 * <p>Usa {@link ScopedValue}: el valor existe solo mientras corre el bloque y no se filtra a otras
 * solicitudes, aun con hilos virtuales. Cada alcance nuevo conserva lo que ya estaba fijado (por
 * ejemplo, entrar a un negocio no olvida a la persona).
 */
public final class TenantContext {

    private static final ScopedValue<Scope> SCOPE = ScopedValue.newInstance();

    private TenantContext() {}

    /**
     * Ejecuta la operación en nombre de una persona. Lo fija la seguridad al resolver la sesión; nunca
     * se lee la sesión desde acá, porque cargarla también usa una conexión.
     */
    public static <T, X extends Throwable> T callAsUser(UUID userId, ScopedValue.CallableOp<T, X> op) throws X {
        Objects.requireNonNull(userId, "userId");
        return ScopedValue.where(SCOPE, current().withUser(userId)).call(op);
    }

    /** Ejecuta la operación dentro de un negocio. */
    public static <T, X extends Throwable> T callInBusiness(UUID businessId, ScopedValue.CallableOp<T, X> op) throws X {
        Objects.requireNonNull(businessId, "businessId");
        return ScopedValue.where(SCOPE, current().withBusiness(businessId)).call(op);
    }

    /**
     * Ejecuta una operación de sistema que necesita ver datos de cualquier negocio (por ejemplo, buscar
     * una invitación por su token antes de saber a qué negocio pertenece). Usar lo menos posible y
     * siempre con un motivo concreto.
     */
    public static <T, X extends Throwable> T callAsSystem(String reason, ScopedValue.CallableOp<T, X> op) throws X {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Una operación de sistema necesita un motivo");
        }
        return ScopedValue.where(SCOPE, current().asSystem(reason)).call(op);
    }

    public static Optional<UUID> currentUser() {
        return Optional.ofNullable(current().userId());
    }

    public static Optional<UUID> currentBusiness() {
        return Optional.ofNullable(current().businessId());
    }

    public static boolean isSystem() {
        return current().systemReason() != null;
    }

    private static Scope current() {
        return SCOPE.isBound() ? SCOPE.get() : Scope.EMPTY;
    }

    private record Scope(UUID userId, UUID businessId, String systemReason) {

        static final Scope EMPTY = new Scope(null, null, null);

        Scope withUser(UUID user) {
            return new Scope(user, businessId, systemReason);
        }

        Scope withBusiness(UUID business) {
            return new Scope(userId, business, null);
        }

        Scope asSystem(String reason) {
            return new Scope(userId, null, reason);
        }
    }
}
