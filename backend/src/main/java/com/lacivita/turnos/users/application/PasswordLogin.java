package com.lacivita.turnos.users.application;

import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.users.domain.PasswordHasher;
import com.lacivita.turnos.users.domain.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Inicio de sesión con email y contraseña. */
@Service
public class PasswordLogin {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final AccountMapper mapper;

    PasswordLogin(UserRepository users, PasswordHasher passwordHasher, AccountMapper mapper) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.mapper = mapper;
    }

    /**
     * Todos los rechazos (email inexistente, cuenta sin contraseña o contraseña incorrecta) responden
     * igual y tardan lo mismo, para no revelar qué emails están registrados.
     *
     * @throws BadCredentialsException si el email o la contraseña no coinciden
     */
    @Transactional(readOnly = true)
    public SignIn authenticate(Email email, String rawPassword) {
        var user = users.findByEmail(email);
        if (user.isEmpty()) {
            passwordHasher.simulateMatch(rawPassword);
            throw new BadCredentialsException("Credenciales inválidas");
        }
        if (!user.get().passwordMatches(rawPassword, passwordHasher)) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
        return mapper.toSignIn(user.get());
    }
}
