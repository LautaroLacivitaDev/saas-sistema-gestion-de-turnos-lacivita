package com.lacivita.turnos.users.application;

import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.users.AccountDirectory;
import com.lacivita.turnos.users.domain.User;
import com.lacivita.turnos.users.domain.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementa las consultas sobre cuentas que el módulo ofrece a otros módulos. */
@Service
class AccountDirectoryService implements AccountDirectory {

    private final UserRepository users;

    AccountDirectoryService(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasVerifiedEmail(UUID userId) {
        return users.findById(userId).map(User::isEmailVerified).orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Email> emailOf(UUID userId) {
        return users.findById(userId).map(User::getEmail);
    }
}
