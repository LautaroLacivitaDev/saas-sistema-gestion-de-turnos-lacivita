package com.lacivita.turnos.users.web;

/** Rutas de la API de autenticación. Las usan el controlador y la declaración de endpoints públicos. */
final class AuthPaths {

    static final String BASE = "/api/auth";
    static final String CSRF = "/csrf";
    static final String REGISTER = "/register";
    static final String LOGIN = "/login";
    static final String LOGIN_LINK = "/login-link";
    static final String LOGIN_LINK_CONSUME = "/login-link/consume";
    static final String EMAIL_VERIFICATION = "/email-verification";
    static final String EMAIL_VERIFICATION_RESEND = "/email-verification/resend";
    static final String ME = "/me";

    private AuthPaths() {}

    static String full(String path) {
        return BASE + path;
    }
}
