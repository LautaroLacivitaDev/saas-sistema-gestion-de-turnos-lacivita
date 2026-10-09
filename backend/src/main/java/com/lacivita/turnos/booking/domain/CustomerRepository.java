package com.lacivita.turnos.booking.domain;

import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.domain.PhoneNumber;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface CustomerRepository extends Repository<Customer, UUID> {

    Customer save(Customer customer);

    Optional<Customer> findById(UUID id);

    List<Customer> findAllByIdIn(Collection<UUID> ids);

    Optional<Customer> findByUserId(UUID userId);

    Optional<Customer> findByEmail(Email email);

    Optional<Customer> findFirstByPhoneOrderByCreatedAtAsc(PhoneNumber phone);
}
