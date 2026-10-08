package com.lacivita.turnos.users.web;

import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.security.SessionAuthenticator;
import com.lacivita.turnos.users.application.AccountQueries;
import com.lacivita.turnos.users.application.AccountRegistration;
import com.lacivita.turnos.users.application.EmailVerification;
import com.lacivita.turnos.users.application.LoginLinks;
import com.lacivita.turnos.users.application.PasswordLogin;
import com.lacivita.turnos.users.application.SignIn;
import com.lacivita.turnos.users.domain.NewPassword;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autenticación con sesión en el servidor. Traduce HTTP a casos de uso: no contiene reglas de negocio.
 *
 * <p>El cierre de sesión ({@code POST /api/auth/logout}) y el login con Google ({@code
 * /api/auth/oauth2/authorization/google}, si está configurado) los resuelve Spring Security.
 */
@Tag(name = "Autenticación")
@RestController
@RequestMapping(AuthPaths.BASE)
class AuthController {

    private final AccountRegistration registration;
    private final PasswordLogin passwordLogin;
    private final LoginLinks loginLinks;
    private final EmailVerification emailVerification;
    private final AccountQueries accounts;
    private final SessionAuthenticator sessions;

    AuthController(
            AccountRegistration registration,
            PasswordLogin passwordLogin,
            LoginLinks loginLinks,
            EmailVerification emailVerification,
            AccountQueries accounts,
            SessionAuthenticator sessions) {
        this.registration = registration;
        this.passwordLogin = passwordLogin;
        this.loginLinks = loginLinks;
        this.emailVerification = emailVerification;
        this.accounts = accounts;
        this.sessions = sessions;
    }

    @Operation(summary = "Obtiene el token CSRF", description = "Lo deja también en la cookie XSRF-TOKEN.")
    @GetMapping(AuthPaths.CSRF)
    AuthResponses.Csrf csrf(CsrfToken token) {
        return new AuthResponses.Csrf(token.getHeaderName(), token.getToken());
    }

    @Operation(summary = "Crea una cuenta con email y contraseña e inicia la sesión")
    @PostMapping(AuthPaths.REGISTER)
    @ResponseStatus(HttpStatus.CREATED)
    AuthResponses.Account register(
            @Valid @RequestBody AuthRequests.Register body, HttpServletRequest request, HttpServletResponse response) {
        var signIn = registration.register(body.name(), new Email(body.email()), new NewPassword(body.password()));
        return startSession(signIn, request, response);
    }

    @Operation(summary = "Inicia sesión con email y contraseña")
    @PostMapping(AuthPaths.LOGIN)
    AuthResponses.Account login(
            @Valid @RequestBody AuthRequests.Login body, HttpServletRequest request, HttpServletResponse response) {
        var signIn = passwordLogin.authenticate(new Email(body.email()), body.password());
        return startSession(signIn, request, response);
    }

    @Operation(
            summary = "Envía un link de acceso por email",
            description = "Responde 202 exista o no la cuenta, para no revelar qué emails están registrados.")
    @PostMapping(AuthPaths.LOGIN_LINK)
    @ResponseStatus(HttpStatus.ACCEPTED)
    void requestLoginLink(@Valid @RequestBody AuthRequests.LoginLink body) {
        loginLinks.request(new Email(body.email()));
    }

    @Operation(summary = "Inicia sesión con un link de acceso")
    @PostMapping(AuthPaths.LOGIN_LINK_CONSUME)
    AuthResponses.Account consumeLoginLink(
            @Valid @RequestBody AuthRequests.Token body, HttpServletRequest request, HttpServletResponse response) {
        return startSession(loginLinks.consume(body.token()), request, response);
    }

    @Operation(summary = "Verifica el email con el link recibido")
    @PostMapping(AuthPaths.EMAIL_VERIFICATION)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void verifyEmail(@Valid @RequestBody AuthRequests.Token body) {
        emailVerification.verify(body.token());
    }

    @Operation(summary = "Reenvía el link de verificación de email")
    @PostMapping(AuthPaths.EMAIL_VERIFICATION_RESEND)
    @ResponseStatus(HttpStatus.ACCEPTED)
    void resendEmailVerification(@AuthenticationPrincipal AuthenticatedUser user) {
        emailVerification.resend(user.id());
    }

    @Operation(summary = "Devuelve la cuenta con sesión iniciada")
    @GetMapping(AuthPaths.ME)
    AuthResponses.Account me(@AuthenticationPrincipal AuthenticatedUser user) {
        return AuthResponses.Account.from(accounts.ofSignedInUser(user.id()));
    }

    private AuthResponses.Account startSession(
            SignIn signIn, HttpServletRequest request, HttpServletResponse response) {
        sessions.signIn(signIn.principal(), request, response);
        return AuthResponses.Account.from(signIn.account());
    }
}
