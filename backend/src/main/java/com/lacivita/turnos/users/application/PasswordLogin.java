package com.lacivita.turnos.users.application;

import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.users.domain.User;
import com.lacivita.turnos.users.domain.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Inicio de sesión con email y contraseña. */
@Service
public class PasswordLogin {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AccountMapper mapper;

    /**
     * Hash contra el que se compara cuando el email no existe o la cuenta no tiene contraseña. Así la
     * respuesta tarda lo mismo en todos los casos y el tiempo no revela qué emails están registrados.
     */
    private final String timingDecoyHash;

    PasswordLogin(UserRepository users, PasswordEncoder passwordEncoder, AccountMapper mapper) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
        this.timingDecoyHash = passwordEncoder.encode("decoy-password-for-timing");
    }

    /** @throws BadCredentialsException si el email o la contraseña no coinciden, sin decir cuál de los dos */
    @Transactional(readOnly = true)
    public SignIn authenticate(Email email, String rawPassword) {
        var user = users.findByEmail(email);
        String hash = user.flatMap(User::passwordHash).orElse(timingDecoyHash);
        boolean matches = passwordEncoder.matches(rawPassword, hash);
        if (!matches || user.isEmpty() || !user.get().hasPassword()) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
        return mapper.toSignIn(user.get());
    }
}
