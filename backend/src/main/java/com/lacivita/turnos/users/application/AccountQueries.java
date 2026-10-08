package com.lacivita.turnos.users.application;

import com.lacivita.turnos.users.domain.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consultas sobre la cuenta propia. */
@Service
public class AccountQueries {

    private final UserRepository users;
    private final AccountMapper mapper;

    AccountQueries(UserRepository users, AccountMapper mapper) {
        this.users = users;
        this.mapper = mapper;
    }

    /** Cuenta de una persona con sesión iniciada. */
    @Transactional(readOnly = true)
    public AccountView ofSignedInUser(UUID userId) {
        return mapper.toView(users.require(userId));
    }
}
