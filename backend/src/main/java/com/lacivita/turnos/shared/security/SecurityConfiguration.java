package com.lacivita.turnos.shared.security;

import com.lacivita.turnos.shared.config.AppProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Seguridad de la API.
 *
 * <ul>
 *   <li>Sesión en el servidor (guardada en PostgreSQL) con cookie HttpOnly y SameSite. Sin JWT en el
 *       navegador.
 *   <li>CSRF activo en modo SPA: el token viaja en la cookie {@code XSRF-TOKEN} y el frontend lo
 *       devuelve en el encabezado {@code X-XSRF-TOKEN}.
 *   <li>Todo lo que está bajo {@code /api} exige sesión salvo los endpoints públicos de autenticación.
 *       Lo que no es de la API se rechaza.
 *   <li>Los permisos por negocio se resuelven con {@code @PreAuthorize} y {@link
 *       BusinessPermissionEvaluator}.
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
class SecurityConfiguration {

    @Bean
    SecurityFilterChain apiSecurity(
            HttpSecurity http,
            SecurityContextRepository contextRepository,
            ObjectProvider<ClientRegistrationRepository> oauthClients,
            ObjectProvider<OidcLoginHandler> oidcLoginHandler,
            AppProperties properties,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {

        var problems = new SecurityProblemHandler(exceptionResolver);

        http.csrf(csrf -> csrf.spa())
                .securityContext(context -> context.securityContextRepository(contextRepository))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(logout -> logout.logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .exceptionHandling(
                        errors -> errors.authenticationEntryPoint(problems).accessDeniedHandler(problems))
                .addFilterBefore(
                        new AuthRateLimitFilter(properties.rateLimit().authRequestsPerMinute(), exceptionResolver),
                        CsrfFilter.class)
                .authorizeHttpRequests(requests -> requests.requestMatchers(
                                HttpMethod.POST, AuthRateLimitFilter.LIMITED_PATHS.toArray(String[]::new))
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf")
                        .permitAll()
                        .requestMatchers("/api/docs/**", "/api/openapi/**", "/swagger-ui/**")
                        .permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/error")
                        .permitAll()
                        .requestMatchers("/actuator/**")
                        .hasRole(PlatformRole.ADMIN.name())
                        .requestMatchers("/api/**")
                        .authenticated()
                        .anyRequest()
                        .denyAll());

        // El login con Google se activa solo si están configuradas las credenciales (perfil "google").
        if (oauthClients.getIfAvailable() != null) {
            var handler = oidcLoginHandler.getObject();
            http.oauth2Login(
                    oauth -> oauth.authorizationEndpoint(endpoint -> endpoint.baseUri("/api/auth/oauth2/authorization"))
                            .redirectionEndpoint(endpoint -> endpoint.baseUri("/api/auth/oauth2/code/*"))
                            .successHandler(handler)
                            .failureHandler(handler));
        }
        return http.build();
    }

    @Bean
    OidcLoginHandler oidcLoginHandler(
            ExternalIdentityResolver identities, SessionAuthenticator sessions, AppProperties properties) {
        return new OidcLoginHandler(identities, sessions, properties);
    }

    /**
     * Cookie de sesión de Spring Session. Se declara explícitamente: las propiedades {@code
     * server.servlet.session.cookie.*} no se aplican a Spring Session y la cookie salía sin HttpOnly ni
     * SameSite (lo detectó AuthApiIntegrationTests).
     *
     * <p>{@code Secure} se decide por solicitud: se agrega cuando la conexión es HTTPS (en producción,
     * detrás del proxy, gracias a los encabezados X-Forwarded-*).
     */
    @Bean
    CookieSerializer sessionCookieSerializer() {
        var serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        serializer.setCookiePath("/");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite("Lax");
        return serializer;
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // Codificador delegante: hoy BCrypt, y permite migrar de algoritmo sin invalidar contraseñas.
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role(PlatformRole.ADMIN.name())
                .implies(PlatformRole.USER.name())
                .build();
    }

    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            RoleHierarchy roleHierarchy, ObjectProvider<BusinessRoleResolver> roles) {
        // El resolver se busca recién al primer chequeo: la seguridad de métodos se arma muy temprano y
        // no debe forzar la creación de los repositorios JPA.
        BusinessRoleResolver lazyRoles =
                (userId, businessId) -> roles.getObject().roleOf(userId, businessId);
        var handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        handler.setPermissionEvaluator(new BusinessPermissionEvaluator(lazyRoles));
        return handler;
    }
}
