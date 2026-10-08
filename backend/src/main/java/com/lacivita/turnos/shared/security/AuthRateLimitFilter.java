package com.lacivita.turnos.shared.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Limita los intentos por IP en los endpoints públicos de autenticación, para frenar ataques de fuerza
 * bruta y el envío masivo de emails.
 */
// DECISIÓN: los contadores viven en memoria de cada instancia (Caffeine). Con varias instancias el
// límite efectivo se multiplica; si hace falta, se pasa a Bucket4j sobre PostgreSQL en el Hito 10.
class AuthRateLimitFilter extends OncePerRequestFilter {

    static final Set<String> LIMITED_PATHS = Set.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/login-link",
            "/api/auth/login-link/consume",
            "/api/auth/email-verification");

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final int requestsPerMinute;
    private final HandlerExceptionResolver exceptionResolver;
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(WINDOW.multipliedBy(10))
            .maximumSize(100_000)
            .build();

    AuthRateLimitFilter(int requestsPerMinute, HandlerExceptionResolver exceptionResolver) {
        this.requestsPerMinute = requestsPerMinute;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod()) || !LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var bucket = buckets.get(request.getRemoteAddr() + "|" + request.getRequestURI(), key -> newBucket());
        var probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            chain.doFilter(request, response);
            return;
        }
        var retryAfter = Duration.ofNanos(probe.getNanosToWaitForRefill());
        exceptionResolver.resolveException(request, response, null, new TooManyRequestsException(retryAfter));
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(requestsPerMinute)
                        .refillGreedy(requestsPerMinute, WINDOW)
                        .build())
                .build();
    }
}
