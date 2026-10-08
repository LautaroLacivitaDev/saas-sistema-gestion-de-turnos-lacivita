package com.lacivita.turnos.shared.tenancy;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un caso de uso que opera sobre un negocio. El negocio es el parámetro anotado con {@link
 * BusinessId}. El contexto se fija antes de la transacción y de los chequeos de permisos:
 *
 * <pre>{@code
 * @BusinessScoped
 * @Transactional
 * @PreAuthorize("hasPermission(#businessId, 'Business', 'OWNER')")
 * public void rename(@BusinessId UUID businessId, String name) { ... }
 * }</pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BusinessScoped {}
