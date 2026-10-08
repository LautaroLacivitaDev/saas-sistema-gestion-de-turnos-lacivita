package com.lacivita.turnos.users.domain;

import com.lacivita.turnos.shared.domain.Email;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

// DECISIÓN: los repositorios son interfaces de Spring Data declaradas en el dominio, con solo los
// métodos que el dominio necesita (extienden Repository, no JpaRepository). Evita una capa de
// adaptadores que solo delegaría.
public interface UserRepository extends Repository<User, UUID> {

    User save(User user);

    /** Guarda y escribe enseguida, para detectar una violación de unicidad dentro del caso de uso. */
    User saveAndFlush(User user);

    Optional<User> findById(UUID id);

    /**
     * Para ids que vienen de una sesión o de un token válidos. Si la cuenta no existe es una
     * inconsistencia del sistema, no un error del usuario.
     */
    default User require(UUID id) {
        return findById(id).orElseThrow(() -> new IllegalStateException("No existe la cuenta " + id));
    }

    Optional<User> findByEmail(Email email);

    List<User> findAllByIdIn(Collection<UUID> ids);

    boolean existsByEmail(Email email);

    @Query("""
            select u from User u join u.identities i
            where i.provider = :provider and i.subject = :subject
            """)
    Optional<User> findByIdentity(@Param("provider") IdentityProvider provider, @Param("subject") String subject);
}
