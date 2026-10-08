package com.lacivita.turnos.users.domain;

/** Hasher para pruebas unitarias: predecible y sin el costo de BCrypt. */
class FakePasswordHasher implements PasswordHasher {

    private int simulatedMatches;

    @Override
    public String hash(NewPassword password) {
        return "hashed:" + password.value();
    }

    @Override
    public boolean matches(String rawPassword, String storedHash) {
        return storedHash.equals("hashed:" + rawPassword);
    }

    @Override
    public void simulateMatch(String rawPassword) {
        simulatedMatches++;
    }

    int simulatedMatches() {
        return simulatedMatches;
    }
}
