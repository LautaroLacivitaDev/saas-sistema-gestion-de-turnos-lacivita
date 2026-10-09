package com.lacivita.turnos.catalog.web;

import com.lacivita.turnos.catalog.domain.PriceRange;
import com.lacivita.turnos.catalog.domain.ServiceDetails;
import com.lacivita.turnos.catalog.domain.ServiceDuration;
import com.lacivita.turnos.shared.domain.Money;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Cuerpos de las solicitudes del catálogo y su traducción a objetos del dominio. */
final class CatalogRequests {

    private CatalogRequests() {}

    record ServiceData(
            @NotBlank(message = "Ingresá el nombre del servicio.") @Size(max = 80)
            String name,

            @NotBlank(message = "Elegí una categoría.") @Size(max = 40)
            String category,

            @Size(max = 500) String description,

            @NotNull(message = "Ingresá la duración.") Integer baseDurationMinutes,

            @NotNull(message = "Ingresá el precio.") BigDecimal basePrice) {

        ServiceDetails details() {
            return new ServiceDetails(
                    name, category, description, new ServiceDuration(baseDurationMinutes), new Money(basePrice));
        }
    }

    /** Rango de precios. Los dos vacíos quitan el rango. */
    record PriceRangeData(BigDecimal min, BigDecimal max) {

        PriceRange range() {
            if (min == null && max == null) {
                return null;
            }
            return new PriceRange(min == null ? null : new Money(min), max == null ? null : new Money(max));
        }
    }

    record ServiceAvailability(@NotNull Boolean active) {}

    record ComboData(
            @NotBlank(message = "Ingresá el nombre del combo.") @Size(max = 80)
            String name,

            @NotNull(message = "Elegí los servicios del combo.")
            List<UUID> serviceIds,

            Boolean active) {

        boolean isActive() {
            return active == null || active;
        }
    }

    /** Precio y duración propios de un profesional. Vacíos: hereda los valores base del servicio. */
    record OfferingTerms(BigDecimal price, Integer durationMinutes) {

        Money ownPrice() {
            return price == null ? null : new Money(price);
        }

        ServiceDuration ownDuration() {
            return durationMinutes == null ? null : new ServiceDuration(durationMinutes);
        }
    }
}
