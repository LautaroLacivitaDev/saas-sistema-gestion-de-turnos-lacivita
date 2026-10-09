package com.lacivita.turnos.catalog.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Modelos de lectura del catálogo. Nunca exponen entidades. */
public final class CatalogViews {

    private CatalogViews() {}

    /**
     * Servicio del catálogo para el equipo.
     *
     * @param priceMin mínimo del rango de precios; nulo si el servicio no tiene rango
     * @param proposedBy quién lo propuso; nulo si lo creó un gerente
     */
    public record ServiceView(
            UUID id,
            String name,
            String category,
            String description,
            int baseDurationMinutes,
            BigDecimal basePrice,
            BigDecimal priceMin,
            BigDecimal priceMax,
            String status,
            UUID proposedBy) {}

    public record ComboView(UUID id, String name, List<UUID> serviceIds, boolean active) {}

    /**
     * Un servicio que hace un barbero.
     *
     * @param ownPrice precio propio; nulo si hereda el base
     * @param price precio vigente (el propio o el base)
     * @param requestedPrice precio fuera de rango esperando aprobación; nulo si no hay pedido
     */
    public record OfferingView(
            UUID id,
            UUID barberId,
            UUID serviceId,
            String serviceName,
            BigDecimal ownPrice,
            Integer ownDurationMinutes,
            BigDecimal price,
            int durationMinutes,
            BigDecimal requestedPrice) {}

    /** Resultado de un cambio de precio: si se aplicó o espera aprobación. */
    public record TermsChange(OfferingView offering, String outcome) {}

    /** Pedido de precio fuera de rango, para que lo revise un gerente. */
    public record PriceRequestView(
            UUID offeringId,
            UUID barberId,
            String barberName,
            UUID serviceId,
            String serviceName,
            BigDecimal currentPrice,
            BigDecimal requestedPrice,
            BigDecimal priceMin,
            BigDecimal priceMax,
            Instant requestedAt) {}

    /** Precio y duración de un profesional en un servicio o combo de la página pública. */
    public record BarberTermsView(UUID barberId, String barberName, BigDecimal price, int durationMinutes) {}

    /**
     * Servicio en la página pública. Solo aparecen los que hace al menos un profesional.
     *
     * @param fromPrice el precio más bajo entre los profesionales ("desde")
     */
    public record PublicServiceView(
            UUID id,
            String name,
            String category,
            String description,
            BigDecimal fromPrice,
            List<BarberTermsView> barbers) {}

    public record PublicComboView(
            UUID id, String name, List<UUID> serviceIds, BigDecimal fromPrice, List<BarberTermsView> barbers) {}

    public record PublicCatalogView(List<PublicServiceView> services, List<PublicComboView> combos) {}

    /** Perfil de un profesional, tal como lo edita. */
    public record ProfileView(String bio, List<String> specialties, String photoUrl) {}

    /** Un servicio que hace un profesional, con su precio y su duración. */
    public record ProfessionalServiceView(UUID serviceId, String name, BigDecimal price, int durationMinutes) {}

    /**
     * Un profesional en la página pública: quién es, dónde atiende y qué hace.
     *
     * @param branchIds sucursales donde atiende; vacío si atiende en todas (el dueño)
     * @param services del más barato al más caro
     */
    public record ProfessionalView(
            UUID barberId,
            String name,
            String bio,
            List<String> specialties,
            String photoUrl,
            List<UUID> branchIds,
            List<ProfessionalServiceView> services) {}
}
