package com.lacivita.turnos.booking.web;

import com.lacivita.turnos.booking.application.BookingViews.CustomerDetailView;
import com.lacivita.turnos.booking.application.BookingViews.CustomerSummaryView;
import com.lacivita.turnos.booking.application.CustomerBook;
import com.lacivita.turnos.booking.domain.CustomerNotes;
import com.lacivita.turnos.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Clientes")
@RestController
@RequestMapping("/api/businesses/{businessId}/customers")
class CustomersController {

    private static final int MAX_SIZE = 50;

    private final CustomerBook customers;

    CustomersController(CustomerBook customers) {
        this.customers = customers;
    }

    @Operation(
            summary = "Busca clientes",
            description = "Gerentes y dueño. Por nombre, email o teléfono (por partes y sin tildes); sin q, todos."
                    + " Con cuántos turnos tuvo cada uno y el último.")
    @GetMapping
    PageResponse<CustomerSummaryView> search(
            @PathVariable UUID businessId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE));
        return PageResponse.of(customers.search(businessId, q, pageable), customer -> customer);
    }

    @Operation(summary = "Ficha de un cliente", description = "Gerentes y dueño. Con sus últimos 50 turnos.")
    @GetMapping("/{customerId}")
    CustomerDetailView detail(@PathVariable UUID businessId, @PathVariable UUID customerId) {
        return customers.detail(businessId, customerId);
    }

    @Operation(
            summary = "Corrige el contacto y las notas de un cliente",
            description = "Gerentes y dueño. 409 customer_email_taken si otro cliente ya tiene ese email.")
    @PutMapping("/{customerId}")
    CustomerDetailView update(
            @PathVariable UUID businessId, @PathVariable UUID customerId, @Valid @RequestBody CustomerUpdate body) {
        return customers.update(
                businessId, customerId, body.contact().contact(), new CustomerNotes(body.notes(), body.preferences()));
    }

    /** @param contact nombre, email y teléfono (al menos uno de los dos últimos) */
    record CustomerUpdate(
            @NotNull(message = "Ingresá los datos del cliente.") @Valid
            BookingRequests.CustomerData contact,

            String notes,
            String preferences) {}
}
