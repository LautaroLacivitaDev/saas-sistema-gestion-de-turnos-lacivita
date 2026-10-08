# MEMORY.md — Diario de Estudio
Memoria del proyecto entre sesiones. Máximo ~50 líneas: resume o elimina lo que ya no
aporte. Plan completo en `docs/plan-mvp.md`; todas las decisiones en `docs/decisiones.md`.
## Estado actual
- Hito 1 (base del proyecto) terminado el 2026-10-08: backend `verify` con 9 pruebas OK y
  frontend con lint, tipos, pruebas y build OK. Probado de punta a punta.
- Próximo: Hito 2, usuarios y autenticación.
- `main` publicado en GitHub. CI (backend y frontend) en verde desde el commit 7a4c28f.
- Repo: https://github.com/LautaroLacivitaDev/saas-sistema-gestion-de-turnos-lacivita
## Decisiones (y por qué)
- Roles en dos niveles: Spring Security solo `ADMIN`/usuario; `OWNER`/`MANAGER`/`BARBER`
  por membresía en cada negocio vía `PermissionEvaluator` (una persona puede ser dueña de
  un negocio y cliente de otro).
- Login con Google al final del Hito 2: faltan credenciales OAuth de Google Cloud.
- Solo email (sin WhatsApp) y sin rol de recepción: alcance acotado.
- Turno `HOLD` de 5 min como bloqueo temporal: lo cubre la restricción de exclusión.
- Disponibilidad con SQL nativo (`JdbcClient`), auditoría en tabla propia: más simple.
- Feriados a mano, combos con un solo profesional, panel del dueño sin ingresos (Fase 2).
## Aprendizajes y errores a evitar
- Nunca editar una migración ya aplicada, ni un comentario: Flyway valida el checksum.
- PostgreSQL de Docker va en el 5433: la PC ya tiene otro PostgreSQL en el 5432.
- Spring Boot 4 movió las anotaciones de prueba web a
  `org.springframework.boot.webmvc.test.autoconfigure` (`WebMvcTest`, `AutoConfigureMockMvc`).
- Modulith JPA necesita la tabla `event_publication`; con `ddl-auto: validate` va por Flyway.
- Next.js 16 cambió APIs: leer `frontend/node_modules/next/dist/docs/` antes de escribir.
- `@vitejs/plugin-react` choca con Babel de Next: no hace falta, Vite 8 compila JSX.
- Next genera tipos globales (`LayoutProps`) en `.next/`: el `typecheck` corre `next typegen`
  antes de `tsc`. Probar siempre sin `.next/` para reproducir el CI.
- En Windows, git no marca `mvnw` como ejecutable: `git update-index --chmod=+x`.
- Docker Desktop necesita WSL 2 y un reinicio real ("Apagar" no aplica los cambios).
## Próximos pasos
- Lautaro: instalar el plugin palantir-java-format en IntelliJ y crear las credenciales
  OAuth de Google para el login.
- Arrancar el Hito 2.
