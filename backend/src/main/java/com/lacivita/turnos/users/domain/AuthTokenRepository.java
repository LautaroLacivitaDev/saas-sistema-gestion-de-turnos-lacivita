package com.lacivita.turnos.users.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface AuthTokenRepository extends Repository<AuthToken, UUID> {

    AuthToken save(AuthToken token);

    Optional<AuthToken> findByTokenHash(String tokenHash);
}
