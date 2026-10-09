package com.lacivita.turnos.catalog;

import java.util.Objects;
import java.util.UUID;

/** Lo que reserva un cliente: un servicio del catálogo o un combo. */
public sealed interface BookableItem {

    record ServiceItem(UUID serviceId) implements BookableItem {

        public ServiceItem {
            Objects.requireNonNull(serviceId, "serviceId");
        }
    }

    record ComboItem(UUID comboId) implements BookableItem {

        public ComboItem {
            Objects.requireNonNull(comboId, "comboId");
        }
    }
}
