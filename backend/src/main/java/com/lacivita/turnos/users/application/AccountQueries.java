package com.lacivita.turnos.users.application;

import com.lacivita.turnos.users.domain.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountQueries {

    private final UserRepository users;
    private final AccountMapper mapper;

    AccountQueries(UserRepository users, AccountMapper mapper) {
        this.users = users;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Optional<AccountView> find(UUID userId) {
        return users.findById(userId).map(mapper::toView);
    }
}
