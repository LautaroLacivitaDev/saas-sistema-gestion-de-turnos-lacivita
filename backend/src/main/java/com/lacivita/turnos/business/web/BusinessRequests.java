package com.lacivita.turnos.business.web;

import com.lacivita.turnos.business.domain.Address;
import com.lacivita.turnos.business.domain.BranchDetails;
import com.lacivita.turnos.business.domain.BusinessCategory;
import com.lacivita.turnos.business.domain.BusinessProfile;
import com.lacivita.turnos.business.domain.Coordinates;
import com.lacivita.turnos.shared.domain.InvalidValueException;
import com.lacivita.turnos.shared.domain.PhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.ZoneId;

/** Cuerpos de las solicitudes del módulo de negocios y su traducción a objetos del dominio. */
final class BusinessRequests {

    /** Zona por defecto para sucursales nuevas: el producto apunta primero a Argentina. */
    static final String DEFAULT_TIME_ZONE = "America/Argentina/Buenos_Aires";

    private BusinessRequests() {}

    record Register(
            @NotBlank(message = "Ingresá el nombre del negocio.") @Size(max = 80)
            String name,

            @NotBlank(message = "Elegí el link del negocio.") @Size(max = 50)
            String slug,

            @NotNull(message = "Elegí el rubro.") BusinessCategory category,
            @Size(max = 1000) String description,
            Boolean searchable) {

        BusinessProfile profile() {
            return new BusinessProfile(name, category, description, searchable == null || searchable);
        }
    }

    record UpdateProfile(
            @NotBlank(message = "Ingresá el nombre del negocio.") @Size(max = 80)
            String name,

            @NotNull(message = "Elegí el rubro.") BusinessCategory category,
            @Size(max = 1000) String description,
            @NotNull Boolean searchable) {

        BusinessProfile profile() {
            return new BusinessProfile(name, category, description, searchable);
        }
    }

    record ChangeSlug(
            @NotBlank(message = "Elegí el link del negocio.") @Size(max = 50)
            String slug) {}

    record BranchData(
            @NotBlank(message = "Ingresá el nombre de la sucursal.") @Size(max = 80)
            String name,

            @NotBlank(message = "Ingresá la dirección.") @Size(max = 150)
            String street,

            @Size(max = 80) String neighborhood,

            @NotBlank(message = "Ingresá la ciudad.") @Size(max = 80)
            String city,

            BigDecimal latitude,
            BigDecimal longitude,
            @Size(max = 30) String phone,
            @Size(max = 60) String timeZone) {

        BranchDetails details() {
            return new BranchDetails(
                    name, new Address(street, neighborhood, city), coordinates(), phoneNumber(), zone());
        }

        private Coordinates coordinates() {
            return latitude == null && longitude == null ? null : new Coordinates(latitude, longitude);
        }

        private PhoneNumber phoneNumber() {
            return phone == null || phone.isBlank() ? null : new PhoneNumber(phone);
        }

        private ZoneId zone() {
            String id = timeZone == null || timeZone.isBlank() ? DEFAULT_TIME_ZONE : timeZone.strip();
            try {
                return ZoneId.of(id);
            } catch (DateTimeException ex) {
                throw new InvalidValueException("invalid_time_zone", "La zona horaria no es válida.");
            }
        }
    }
}
