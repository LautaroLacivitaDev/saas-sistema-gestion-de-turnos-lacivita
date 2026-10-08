package com.lacivita.turnos.shared.web;

import com.lacivita.turnos.shared.domain.ConflictException;
import com.lacivita.turnos.shared.domain.DomainException;
import com.lacivita.turnos.shared.domain.ForbiddenException;
import com.lacivita.turnos.shared.domain.InvalidValueException;
import com.lacivita.turnos.shared.domain.NotFoundException;
import com.lacivita.turnos.shared.domain.RuleViolationException;
import com.lacivita.turnos.shared.security.TooManyRequestsException;
import java.util.LinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Manejador global de errores de la API. Todas las respuestas de error usan Problem Details (RFC
 * 9457).
 *
 * <p>Las excepciones de dominio llevan un {@code code} estable que se agrega a la respuesta para que el
 * frontend reaccione sin depender del texto. Las excepciones de Spring MVC las resuelve {@link
 * ResponseEntityExceptionHandler}. Cualquier otra excepción se responde como 500 sin exponer detalles
 * internos.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomain(DomainException ex) {
        HttpStatus status = switch (ex) {
            case NotFoundException ignored -> HttpStatus.NOT_FOUND;
            case ConflictException ignored -> HttpStatus.CONFLICT;
            case RuleViolationException ignored -> HttpStatus.UNPROCESSABLE_CONTENT;
            case ForbiddenException ignored -> HttpStatus.FORBIDDEN;
            case InvalidValueException ignored -> HttpStatus.BAD_REQUEST;
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setProperty("code", ex.code());
        return problem;
    }

    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "invalid_credentials", "El email o la contraseña no son correctos.");
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleAuthentication(AuthenticationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "authentication_required", "Necesitás iniciar sesión.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "access_denied", "No tenés permiso para hacer esto.");
    }

    @ExceptionHandler(TooManyRequestsException.class)
    ResponseEntity<ProblemDetail> handleTooManyRequests(TooManyRequestsException ex) {
        long seconds = Math.max(1, ex.retryAfter().toSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(seconds))
                .body(problem(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "too_many_requests",
                        "Hiciste demasiados intentos. Esperá un momento y probá de nuevo."));
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Error no controlado", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intentá de nuevo más tarde.");
        problem.setTitle("Error interno");
        return problem;
    }

    /** Errores de Bean Validation: devuelve el detalle de cada campo inválido. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var fieldErrors = new LinkedHashMap<String, String>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "validation_failed", "Revisá los datos ingresados.");
        problem.setProperty("errors", fieldErrors);
        return ResponseEntity.badRequest().headers(headers).body(problem);
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code);
        return problem;
    }
}
