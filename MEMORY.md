# MEMORY.md — Diario de Estudio
Memoria del proyecto entre sesiones. Máximo ~50 líneas: resume o elimina lo que ya no
aporte. Plan completo en `docs/plan-mvp.md`; todas las decisiones en `docs/decisiones.md`.
## Estado actual
- La app se llama **Laciturnos** (interfaz, emails, docs). Paquete, base y contenedores siguen `turnos`.
- Hitos 1 a 7 terminados (2026-10-09). Hito 7: notificaciones (outbox, JobRunr, recordatorios,
  resumen diario, textos del negocio, .ics, avisos en la app). 330 pruebas OK y Docker.
- **Mobile first**: la app se usa sobre todo desde el celular. Reglas en AGENTS.md; cada pantalla
  se prueba a 320 y 375 px y en escritorio antes de cerrar un hito.
- Login con Google probado solo con proveedor simulado; falta Google real (perfil `google`).
- Próximo: Hito 8, frontend público (buscador, `/{slug}`, flujo de reserva, `/turno?token=`).
- Repo: https://github.com/LautaroLacivitaDev/saas-sistema-gestion-de-turnos-lacivita
## Decisiones (y por qué)
- Roles en dos niveles: Spring Security solo `ADMIN`/`USER`; `OWNER`/`MANAGER`/`BARBER` por
  membresía, vía `BusinessMembershipResolver` (shared) que implementa `users`.
- Sesión en PostgreSQL (Spring Session JDBC): servidores sin estado; lo que va en ella es `Serializable`.
- Aislamiento en dos barreras: `@TenantId` de Hibernate + RLS. La app usa `turnos_app` (sin
  privilegios), Flyway el dueño. `TenantContext` (negocio, persona, sistema) se fija antes de la
  transacción con `@BusinessScoped`; la persona la fija `SignedInUserScopeFilter`.
- Cruzar negocios solo con `TenantContext.callAsSystem(motivo, …)`.
- Auditoría síncrona en la misma transacción; el acceso de soporte exige `X-Support-Reason`.
- Tokens de email: solo el SHA-256. Avisos de turnos por la bandeja `notification` (misma
  transacción, se arman al enviar); emails de cuenta y código del invitado, directo y sin reintentos.
- JobRunr 8.8.2: sus tablas las crea Flyway (V14, `skip-create`); al actualizarlo, migrar a mano.
- Entre módulos: consultas por la API pública (`TeamDirectory`, `BusinessDirectory`) y efectos
  por eventos síncronos en la misma transacción (`MemberLeft`), así heredan el `TenantContext`.
- Agenda: los turnos tomados entran por `BookedTimes` (lo implementa `booking`); la disponibilidad
  cruza horario del profesional y de la sucursal, y los arma en hora local con la zona de la sucursal.
- Precios: un barbero fuera del rango deja un pedido y rige su precio anterior; gerente y dueño
  aplican directo. Combo = suma de las condiciones de un mismo profesional.
- Rate limit por IP en memoria (Caffeine + Bucket4j); distribuido si hace falta en Hito 10.
## Aprendizajes y errores a evitar
- Nunca editar una migración ya commiteada, ni un comentario: Flyway valida el checksum.
- PostgreSQL de Docker va en el 5433: la PC ya tiene otro PostgreSQL en el 5432.
- Boot 4: anotaciones de prueba web en `org.springframework.boot.webmvc.test.autoconfigure`;
  `server.servlet.session.cookie.*` no se aplica a Spring Session (bean `CookieSerializer`).
- Hibernate valida tipos: `VARCHAR`, no `CHAR`, para columnas `String`.
- Nunca leer `SecurityContextHolder` al entregar una conexión: carga la sesión desde la base,
  pide otra conexión y agota el pool (se diagnosticó con `leak-detection-threshold` de Hikari).
- Id propio: `@Version Long` (no `long`) o `Persistable`, si no Spring Data hace `merge` en las altas.
- Entidad ya cargada: `flush()`, no `saveAndFlush` (el `merge` falla con hijos nuevos).
- `csrf()` de Spring Security Test cambia el filtro del contexto compartido: usar `SpaCsrf`.
- Constantes `static final` que usa el constructor van ANTES de las que crean instancias
  (pasó dos veces: `Money.ZERO` y `ScheduleRules.DEFAULT`). Propuesto pasarlo a AGENTS.md.
- Flyway "more than one migration with version": migración vieja en `target/`, `mvnw clean verify`.
- Si un cambio de CSS no se ve en `npm run dev`, borrar `.next/` (caché de Turbopack).
- Next.js 16 cambió APIs: leer `frontend/node_modules/next/dist/docs/` antes de escribir.
## Próximos pasos
- Lautaro: crear las credenciales OAuth de Google y probar el login real.
- Emails de producción por Resend (SMTP, ya configurado). Lautaro: cuenta, dominio verificado y API key.
