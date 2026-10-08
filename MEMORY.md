# MEMORY.md — Diario de Estudio
Memoria del proyecto entre sesiones. Máximo ~50 líneas: resume o elimina lo que ya no
aporte. Plan completo en `docs/plan-mvp.md`; todas las decisiones en `docs/decisiones.md`.
## Estado actual
- La app se llama **Laciturnos** (interfaz, emails, docs). Paquete, base y contenedores siguen `turnos`.
- Hitos 1, 2 y 3 terminados (2026-10-08). Hito 3: negocios, slug con redirección, sucursales,
  equipo con invitaciones, RLS, auditoría y acceso de soporte del ADMIN. 180 pruebas OK y
  probado contra Docker.
- Login con Google probado solo con proveedor simulado; falta Google real (perfil `google`).
- Próximo: Hito 4, catálogo.
- Repo: https://github.com/LautaroLacivitaDev/saas-sistema-gestion-de-turnos-lacivita
## Decisiones (y por qué)
- Roles en dos niveles: Spring Security solo `ADMIN`/`USER`; `OWNER`/`MANAGER`/`BARBER` por
  membresía, vía `BusinessMembershipResolver` (shared) que implementa `users`.
- Sesión en PostgreSQL (Spring Session JDBC): servidores sin estado.
- Aislamiento en dos barreras: `@TenantId` de Hibernate + RLS. La app usa `turnos_app` (sin
  privilegios), Flyway el dueño. `TenantContext` (negocio, persona, sistema) se fija antes de la
  transacción con `@BusinessScoped`; la persona la fija `SignedInUserScopeFilter`.
- Cruzar negocios solo con `TenantContext.callAsSystem(motivo, …)`.
- Auditoría síncrona en la misma transacción; el acceso de soporte exige `X-Support-Reason`.
- Tokens de email: solo el SHA-256, un uso. Emails después del commit y sin reintentos hasta
  el outbox del Hito 7.
- Rate limit por IP en memoria (Caffeine + Bucket4j); distribuido si hace falta en Hito 10.
## Aprendizajes y errores a evitar
- Nunca editar una migración ya commiteada, ni un comentario: Flyway valida el checksum.
- PostgreSQL de Docker va en el 5433: la PC ya tiene otro PostgreSQL en el 5432.
- Boot 4: anotaciones de prueba web en `org.springframework.boot.webmvc.test.autoconfigure`;
  `server.servlet.session.cookie.*` no se aplica a Spring Session (bean `CookieSerializer`).
- Todo lo que va en la sesión tiene que ser `Serializable`.
- Hibernate valida tipos: `VARCHAR`, no `CHAR`, para columnas `String`.
- Nunca leer `SecurityContextHolder` al entregar una conexión: carga la sesión desde la base,
  pide otra conexión y agota el pool (se diagnosticó con `leak-detection-threshold` de Hikari).
- `@Version` como `Long`, no `long`: con id propio, Spring Data hacía `merge` en las altas.
- Entidad ya cargada: `flush()`, no `saveAndFlush` (el `merge` falla con hijos nuevos).
- `csrf()` de Spring Security Test cambia el filtro del contexto compartido: usar `SpaCsrf`.
- Spring ya no copia el mensaje de PostgreSQL en la excepción: mirar `rootCause()`.
- En pruebas, las colecciones lazy se leen dentro de `TransactionTemplate`.
- Next.js 16 cambió APIs: leer `frontend/node_modules/next/dist/docs/` antes de escribir.
- En Windows, git no marca `mvnw` como ejecutable: `git update-index --chmod=+x`.
## Próximos pasos
- Lautaro: crear las credenciales OAuth de Google y probar el login real.
- Hito 4: servicios, combos (un solo profesional) y precios por barbero.
