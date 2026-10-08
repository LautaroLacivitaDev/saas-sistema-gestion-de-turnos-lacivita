package com.lacivita.turnos.shared.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Manejador global de errores de la API. Todas las respuestas de error usan Problem Details (RFC
 * 9457).
 *
 * <p>Las excepciones de Spring MVC (validación, método no soportado, recurso inexistente, etc.) las
 * resuelve {@link ResponseEntityExceptionHandler}. Cualquier otra excepción se responde como 500 sin
 * exponer detalles internos.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Error no controlado", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intentá de nuevo más tarde.");
        problem.setTitle("Error interno");
        return problem;
    }
}
