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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Limita los intentos por IP en los endpoints públicos marcados como {@link PublicEndpoint#rateLimited()},
 * para frenar ataques de fuerza bruta y el envío masivo de emails.
 */
// DECISIÓN: los contadores viven en memoria de cada instancia (Caffeine). Con varias instancias el
// límite efectivo se multiplica; si hace falta, se pasa a Bucket4j sobre PostgreSQL en el Hito 10.
class PublicEndpointRateLimitFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    /** Endpoints limitados. Las rutas pueden tener comodines (un asterisco por segmento). */
    private final List<LimitedEndpoint> limitedEndpoints;

    private final int requestsPerMinute;
    private final HandlerExceptionResolver exceptionResolver;
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(WINDOW.multipliedBy(10))
            .maximumSize(100_000)
            .build();

    PublicEndpointRateLimitFilter(
            Collection<PublicEndpoint> endpoints, int requestsPerMinute, HandlerExceptionResolver exceptionResolver) {
        this.limitedEndpoints = endpoints.stream()
                .filter(PublicEndpoint::rateLimited)
                .map(LimitedEndpoint::of)
                .toList();
        this.requestsPerMinute = requestsPerMinute;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return matching(request).isEmpty();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Un contador por IP y endpoint (no por URL): pedir códigos para muchos turnos distintos también cuenta.
        var endpoint = matching(request).orElseThrow();
        var bucket = buckets.get(request.getRemoteAddr() + "|" + endpoint.key(), key -> newBucket());
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

    private Optional<LimitedEndpoint> matching(HttpServletRequest request) {
        return limitedEndpoints.stream()
                .filter(endpoint -> endpoint.matcher().matches(request))
                .findFirst();
    }

    private record LimitedEndpoint(String key, RequestMatcher matcher) {

        static LimitedEndpoint of(PublicEndpoint endpoint) {
            return new LimitedEndpoint(
                    endpoint.method().name() + " " + endpoint.path(),
                    PathPatternRequestMatcher.withDefaults().matcher(endpoint.method(), endpoint.path()));
        }
    }
}
