# MEMORY.md — Diario de Estudio
Memoria del proyecto entre sesiones. Máximo ~50 líneas: resume o elimina lo que ya no
aporte. Plan completo en `docs/plan-mvp.md`; todas las decisiones en `docs/decisiones.md`.
## Estado actual
- La app se llama **Laciturnos** (interfaz, emails, docs). Paquete, base y contenedores siguen `turnos`.
- MVP terminado (Hitos 1 a 10). Publicado el 2026-10-14: https://laciturnos.vercel.app (frontend) y
  https://laciturnos-api.onrender.com (backend). Render se duerme sin uso: la primera visita tarda ~1 min.
- **Portfolio**: Vercel + Render (Docker) + Neon, sin dominio propio. Perfil `demo`: emails al buzón
  `/demo/emails`, "Barbería El Faro" de ejemplo (se reinicia en cada arranque), cuentas con `demo1234`.
- **Mobile first**: cada pantalla se prueba a 320 y 375 px y en escritorio antes de cerrar un hito.
- Login con Google probado solo con proveedor simulado; para Google real faltan credenciales OAuth.
- Repo: https://github.com/LautaroLacivitaDev/saas-sistema-gestion-de-turnos-lacivita
## Decisiones (y por qué)
- Roles en dos niveles: Spring Security solo `ADMIN`/`USER`; `OWNER`/`MANAGER`/`BARBER` por
  membresía, vía `BusinessMembershipResolver` (shared) que implementa `users`.
- Sesión en PostgreSQL (Spring Session JDBC): servidores sin estado; lo que va en ella es `Serializable`.
- Aislamiento: `@TenantId` + RLS. La app usa `turnos_app` (sin privilegios), Flyway el dueño.
  `TenantContext` se fija antes de la transacción con `@BusinessScoped`. Cruzar negocios solo con
  `TenantContext.callAsSystem(motivo, …)`. Neon por conexión directa (el pooler compartiría el contexto).
- Tokens de email: solo el SHA-256. Avisos de turnos por la bandeja `notification`; emails de cuenta
  y código del invitado, directo y sin reintentos. En demo, ambos van a `demo_email`.
- JobRunr 8.8.2: sus tablas las crea Flyway (V14, `skip-create`); al actualizarlo, migrar a mano.
- Entre módulos: consultas por la API pública y efectos por eventos síncronos (heredan el contexto).
- Agenda: escrituras serializadas por profesional (`pg_advisory_xact_lock` por el `EntityManager`);
  la restricción de exclusión sigue impidiendo la doble reserva.
- Detrás de Vercel se confía en `X-Forwarded-For` (límite de intentos por visitante).
## Aprendizajes y errores a evitar
- Nunca editar una migración ya commiteada, ni un comentario: Flyway valida el checksum.
- PostgreSQL de Docker va en el 5433: la PC ya tiene otro PostgreSQL en el 5432.
- Boot 4: anotaciones de prueba web en `org.springframework.boot.webmvc.test.autoconfigure`;
  `server.servlet.session.cookie.*` no se aplica a Spring Session (bean `CookieSerializer`).
- Nunca leer `SecurityContextHolder` al entregar una conexión: agota el pool.
- Id propio: `@Version Long` (no `long`) o `Persistable`, si no Spring Data hace `merge` en las altas.
- `csrf()` de Spring Security Test cambia el filtro del contexto compartido: usar `SpaCsrf`.
- Constantes `static final` que usa el constructor van ANTES de las que crean instancias.
- Horas: con `jdbc.time_zone: UTC` Hibernate corría los `LocalTime` según la zona de la JVM; ahora
  `java_time_use_direct_jdbc` y las pruebas corren en zona de Buenos Aires para que CI lo detecte.
- SQL de Flyway en `INSERT … SELECT`: castear literales (`'…'::UUID`, `'10:00'::TIME`).
- El CSRF del frontend repite el valor de la cookie `XSRF-TOKEN` (no el `token` de `/api/auth/csrf`).
- Si una página queda en "Cargando…" en desarrollo: borrar `.next/`, o el panel del navegador de
  Claude está oculto (suspende la red; reabrir con `preview_start`).
- Next.js 16 cambió APIs: leer `frontend/node_modules/next/dist/docs/` antes de escribir.
## Próximos pasos
- Probar la demo en el celular real (la reserva como invitado pasa por Turnstile: no se automatiza).
- En Neon, `turnos_app` se crea por SQL; su contraseña se cambia con Roles → Reset password (copiar a Render).
- Opcional: Content-Security-Policy con nonce; Google real; dominio propio y Resend si deja de ser demo.
