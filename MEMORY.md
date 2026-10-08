# MEMORY.md

Memoria del proyecto: estado actual, decisiones tomadas y temas abiertos. Los agentes la leen al empezar y la actualizan al cerrar cada hito o al tomar una decisión. Las reglas de trabajo están en [AGENTS.md](AGENTS.md).

## Estado actual

- **Hito en curso:** ninguno. Falta arrancar el Hito 2.
- **Último hito cerrado:** Hito 1 (2026-10-08).
- **Repositorio:** https://github.com/LautaroLacivitaDev/saas-sistema-gestion-de-turnos-lacivita

## Hitos

| # | Hito | Estado |
|---|---|---|
| 1 | Base del proyecto: estructura, Docker Compose, Spring Boot con Modulith, Flyway, CI, documentación | Terminado |
| 2 | Usuarios y autenticación: email y contraseña, Google, sesiones, `PermissionEvaluator` | Pendiente |
| 3 | Negocios y sucursales: slug, sucursales, membresías e invitaciones, RLS, auditoría | Pendiente |
| 4 | Catálogo: servicios, combos, servicios por barbero, rangos de precio y aprobaciones | Pendiente |
| 5 | Agenda y disponibilidad: horarios, descansos, bloqueos, feriados, anti doble reserva | Pendiente |
| 6 | Reservas: flujo con `HOLD`, invitado, estados, cancelación y reprogramación por link | Pendiente |
| 7 | Notificaciones: outbox, plantillas, recordatorios, `.ics`, registro de envíos | Pendiente |
| 8 | Frontend público: buscador, página `/{slug}`, flujo de reserva | Pendiente |
| 9 | Frontend del panel: agenda, catálogo, equipo, sucursales, clientes, onboarding | Pendiente |
| 10 | Endurecimiento: Playwright, k6, observabilidad, seguridad, PWA | Pendiente |

## Decisiones tomadas

### Producto (2026-10-07)

- WhatsApp queda fuera del proyecto hasta terminar el desarrollo. Las notificaciones son solo por email, con una capa de canales para sumar otros más adelante.
- El rol de recepción no se implementa ni se deja previsto.
- El dueño es el único que nombra o quita gerentes. El gerente invita barberos (reciben el rol al aceptar la invitación) y los da de baja.
- El barbero siempre ve su propia agenda. Por defecto también ve las reservas de su sucursal; el dueño puede restringirlo.

### Plan del MVP (2026-10-08)

- Paquete base del backend: `com.lacivita.turnos`.
- Panel del dueño en el MVP: solo cantidad de turnos por estado y ocupación del día por sucursal. Los reportes de ingresos quedan para la Fase 2.
- Feriados: cada sucursal los carga a mano. No se usa una API de feriados nacionales.
- Servicios combinados: los realiza un solo profesional, sumando duración y precio.
- Bloqueo temporal de horario: turno con estado `HOLD` que vence a los 5 minutos, cubierto por la misma restricción de exclusión.
- Barbero modelado como membresía con perfil, por negocio.
- Disponibilidad calculada con SQL nativo (`JdbcClient`) en lugar de jOOQ.
- Auditoría en tabla propia (`audit_log`) alimentada por eventos, en lugar de Envers.
- Estado de emails en el MVP: `ENVIADO` o `FALLIDO`. `ENTREGADO` requiere webhooks del proveedor.
- Avisos en la app para el barbero: lista en el panel con consulta periódica, sin WebSockets.
- En desarrollo, Next.js redirige `/api/*` al backend en `localhost:8080`.
- **Roles en dos niveles:** Spring Security maneja solo los roles de plataforma (`ADMIN` y usuario común). `OWNER`, `MANAGER` y `BARBER` dependen de la membresía en cada negocio y los resuelve el `PermissionEvaluator` con la misma jerarquía. Difiere de la especificación, que pedía `RoleHierarchy` para todos.
- **Login con Google:** se avanza primero con email y contraseña, y las pruebas simulan el proveedor. Google se conecta cuando estén las credenciales OAuth de Google Cloud Console (las crea Lautaro).

### Hito 1 (2026-10-08)

- **Versiones:** Spring Boot 4.1.1 (la estable actual, sin necesidad de bajar a 4.0.x), Spring Modulith 2.1.1, springdoc 3.1.1, Next.js 16.4, React 19.3, Tailwind CSS 4, Vitest 5, PostgreSQL 17.11.
- **PostgreSQL de Docker en el puerto 5433:** la PC de desarrollo ya tiene un PostgreSQL instalado en el 5432.
- **Tabla `event_publication` de Modulith creada con Flyway** (`V2`), no por Modulith, para que el esquema sea versionado. Hibernate valida el esquema (`ddl-auto: validate`).
- **POM agregador en la raíz:** existe solo para que IntelliJ importe el backend al abrir la carpeta. El build real usa el wrapper de `backend/`.
- **Configuraciones de `.run/` para el frontend y Docker Compose:** son de tipo *Shell Script*, porque IntelliJ Community no tiene configuraciones de npm.
- **Vitest sin `@vitejs/plugin-react`:** su versión actual choca con Babel 7 que trae Next. Vite 8 compila JSX/TSX y resuelve los alias por su cuenta.
- **Spotless con palantir-java-format**, ligado a `verify`: el CI falla si el código no tiene formato.
- **CI sin SonarCloud por ahora:** necesita un token de la cuenta. Se puede sumar más adelante.
- **Swagger UI en `/api/docs`** y OpenAPI en `/api/openapi`, desactivados en producción.
- **Perfil por defecto `dev`**, para que el backend arranque desde el IDE sin configurar nada.

## Pendiente de confirmar

Nada por ahora.

## Entorno de desarrollo

Configurado el 2026-10-08 en Windows 11:

- JDK 25 (Eclipse Temurin 25.0.4), con `JAVA_HOME` configurado.
- Node 24, Git 2.53.
- Docker Desktop 29.8 con WSL 2.
- IntelliJ IDEA Community 2025.2 (backend) con el plugin de Claude Code. VS Code para el frontend.

## Registro de hitos

> Al cerrar cada hito, agregar una entrada: fecha, qué quedó hecho, cómo probarlo y decisiones tomadas.

### Hito 1: base del proyecto (2026-10-08)

**Qué quedó hecho**

- Backend Spring Boot 4.1 con los módulos `business`, `users`, `catalog`, `schedule`, `booking`, `notifications` y el módulo abierto `shared`.
- Migraciones Flyway: extensiones `btree_gist`, `pg_trgm` y `unaccent` (V1) y tabla de eventos de Modulith (V2).
- Manejador global de errores con Problem Details, hilos virtuales, perfiles `dev` y `prod`, logs JSON en producción.
- Pruebas: integración con PostgreSQL real (Testcontainers), límites entre módulos (Modulith), reglas de arquitectura (ArchUnit) y manejo de errores. 9 pruebas.
- Frontend Next.js 16 con Tailwind, shadcn/ui, textos centralizados en `src/messages/es-AR.ts`, proxy `/api/*` al backend y Vitest.
- Docker Compose (PostgreSQL y Mailpit), CI en GitHub Actions, `.run/` para IntelliJ, `.editorconfig`, README y AGENTS.md con los comandos.

**Cómo probarlo**

1. `docker compose up -d`
2. En `backend/`: `mvnw.cmd verify` (o `./mvnw verify`) y después `mvnw.cmd spring-boot:run`. Abrir http://localhost:8080/api/docs.
3. En `frontend/`: `npm install` y `npm run dev`. Abrir http://localhost:3000.
4. http://localhost:3000/api/openapi tiene que devolver el documento de la API pasando por el proxy.

Las decisiones del hito están en "Decisiones tomadas → Hito 1".
