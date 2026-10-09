package com.lacivita.turnos.booking.domain;

import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.domain.PhoneNumber;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends Repository<Customer, UUID> {

    Customer save(Customer customer);

    /** Escribe los cambios enseguida: un email repetido lo detecta la base dentro del caso de uso. */
    void flush();

    Optional<Customer> findById(UUID id);

    List<Customer> findAllByIdIn(Collection<UUID> ids);

    Optional<Customer> findByUserId(UUID userId);

    Optional<Customer> findByEmail(Email email);

    Optional<Customer> findFirstByPhoneOrderByCreatedAtAsc(PhoneNumber phone);

    /**
     * Clientes del negocio que coinciden con lo buscado (por partes y sin tildes, en el nombre, el email o el
     * teléfono), ordenados por nombre, con cuántos turnos tuvieron y cuándo fue el último. Sin texto, lista
     * todos. El negocio lo limita Row Level Security.
     *
     * @param text lo buscado, con los comodines de LIKE escapados ({@code \})
     * @param digits los números de lo buscado, para encontrar teléfonos escritos con espacios o guiones
     */
    @Query(nativeQuery = true, value = """
                    SELECT c.id AS id, c.name AS name, c.email AS email, c.phone AS phone,
                           (SELECT count(*) FROM appointment a
                            WHERE a.customer_id = c.id AND a.status <> 'HOLD') AS appointments,
                           (SELECT max(a.starts_at) FROM appointment a
                            WHERE a.customer_id = c.id AND a.status <> 'HOLD') AS lastAppointmentAt
                    FROM customer c
                    WHERE (:text = ''
                           OR app_search_text(c.name || ' ' || coalesce(c.email, '') || ' ' || coalesce(c.phone, ''))
                              LIKE '%' || app_search_text(:text) || '%' ESCAPE '\\'
                           OR (length(:digits) >= 3 AND c.phone LIKE '%' || :digits || '%'))
                    ORDER BY c.name, c.id
                    """, countQuery = """
                    SELECT count(*) FROM customer c
                    WHERE (:text = ''
                           OR app_search_text(c.name || ' ' || coalesce(c.email, '') || ' ' || coalesce(c.phone, ''))
                              LIKE '%' || app_search_text(:text) || '%' ESCAPE '\\'
                           OR (length(:digits) >= 3 AND c.phone LIKE '%' || :digits || '%'))
                    """)
    Page<CustomerRow> search(@Param("text") String text, @Param("digits") String digits, Pageable pageable);

    /** Un cliente en el listado. */
    interface CustomerRow {

        UUID getId();

        String getName();

        String getEmail();

        String getPhone();

        long getAppointments();

        Instant getLastAppointmentAt();
    }
}
