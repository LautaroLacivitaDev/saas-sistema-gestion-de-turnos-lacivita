package com.lacivita.turnos.users.infrastructure;

import com.lacivita.turnos.users.domain.NewPassword;
import com.lacivita.turnos.users.domain.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** {@link PasswordHasher} sobre el {@link PasswordEncoder} delegante de Spring Security (hoy BCrypt). */
@Component
class SpringPasswordHasher implements PasswordHasher {

    private final PasswordEncoder encoder;

    /** Hash real contra el que se compara en {@link #simulateMatch}, para gastar el mismo tiempo. */
    private final String decoyHash;

    SpringPasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
        this.decoyHash = encoder.encode("decoy-password-for-timing");
    }

    @Override
    public String hash(NewPassword password) {
        return encoder.encode(password.value());
    }

    @Override
    public boolean matches(String rawPassword, String storedHash) {
        return encoder.matches(rawPassword, storedHash);
    }

    @Override
    public void simulateMatch(String rawPassword) {
        encoder.matches(rawPassword, decoyHash);
    }
}
