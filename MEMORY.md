# MEMORY.md — Diario de Estudio
Memoria del proyecto entre sesiones. Máximo ~50 líneas: resume o elimina lo que ya no
aporte. Plan completo en `docs/plan-mvp.md`; todas las decisiones en `docs/decisiones.md`.
## Estado actual
- Hito 2 (usuarios y autenticación) terminado el 2026-10-08: 92 pruebas OK y probado de
  punta a punta contra Docker pasando por el proxy de Next (registro, sesión, email, logout).
- Login con Google programado y probado con proveedor simulado; falta probarlo con Google
  real cuando estén las credenciales (perfil `google`, ver README).
- Próximo: Hito 3, negocios y sucursales.
- Repo: https://github.com/LautaroLacivitaDev/saas-sistema-gestion-de-turnos-lacivita
## Decisiones (y por qué)
- Roles en dos niveles: Spring Security solo `ADMIN`/`USER`; `OWNER`/`MANAGER`/`BARBER` por
  membresía, vía `BusinessRoleResolver` (shared) que implementa `users`. Así `shared` no
  depende de ningún módulo.
- Sesión en PostgreSQL (Spring Session JDBC): servidores sin estado, escalan horizontalmente.
- Login con contraseña, link de acceso y Google terminan en el mismo principal
  (`AuthenticatedUser`) vía `SessionAuthenticator`.
- Google sobre cuenta sin verificar: se vincula, se verifica y se descarta la contraseña
  (evita que quien registró un email ajeno conserve el acceso).
- Tokens de email: solo se guarda el SHA-256; un solo uso y vencimiento por tipo.
- Emails de cuenta después del commit y sin reintentos (no se usan eventos persistidos para
  no guardar tokens en claro). El outbox con reintentos llega en el Hito 7.
- Rate limit por IP en memoria (Caffeine + Bucket4j); distribuido si hace falta en Hito 10.
## Aprendizajes y errores a evitar
- Nunca editar una migración ya aplicada, ni un comentario: Flyway valida el checksum.
- PostgreSQL de Docker va en el 5433: la PC ya tiene otro PostgreSQL en el 5432.
- Boot 4: las anotaciones de prueba web están en `org.springframework.boot.webmvc.test.autoconfigure`
  y los starters cambiaron de nombre (`spring-boot-starter-security-oauth2-client`).
- Boot 4 + Spring Session: `server.servlet.session.cookie.*` NO se aplica. La cookie se
  configura con un bean `CookieSerializer`. Verificar siempre el `Set-Cookie` real.
- Todo lo que va en la sesión tiene que ser `Serializable` (se guarda en PostgreSQL).
- Hibernate valida tipos: usar `VARCHAR`, no `CHAR`, para columnas `String`.
- El health de mail marca la app como caída si no hay SMTP: está desactivado a propósito.
- En pruebas, las colecciones lazy se leen dentro de `TransactionTemplate`.
- Next.js 16 cambió APIs: leer `frontend/node_modules/next/dist/docs/` antes de escribir.
- Next genera tipos en `.next/`: `typecheck` corre `next typegen` antes de `tsc`.
- En Windows, git no marca `mvnw` como ejecutable: `git update-index --chmod=+x`.
## Próximos pasos
- Lautaro: crear las credenciales OAuth de Google y probar el login real.
- Arrancar el Hito 3 (incluye auditar el acceso de un ADMIN a un negocio).
