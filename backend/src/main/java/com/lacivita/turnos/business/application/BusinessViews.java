package com.lacivita.turnos.business.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Modelos de lectura del módulo de negocios. Nunca exponen entidades. */
public final class BusinessViews {

    private BusinessViews() {}

    /** Datos del negocio para su equipo. */
    public record BusinessView(
            UUID id, String name, String slug, String category, String description, boolean searchable) {}

    public record BranchView(
            UUID id,
            String name,
            String street,
            String neighborhood,
            String city,
            BigDecimal latitude,
            BigDecimal longitude,
            String phone,
            String timeZone) {}

    /**
     * Página pública de reservas.
     *
     * @param canonicalSlug slug actual. Si es distinto del pedido, el link es viejo y el frontend
     *     redirige al nuevo
     */
    public record PublicBusinessPage(
            String canonicalSlug, String name, String category, String description, List<BranchView> branches) {}

    /**
     * Resultado de verificar un slug.
     *
     * @param code motivo si no está disponible: {@code invalid_slug}, {@code reserved_slug} o {@code
     *     slug_taken}
     */
    public record SlugCheck(String slug, boolean available, String code, String message) {

        static SlugCheck available(String slug) {
            return new SlugCheck(slug, true, null, null);
        }

        static SlugCheck unavailable(String slug, String code, String message) {
            return new SlugCheck(slug, false, code, message);
        }
    }
}
