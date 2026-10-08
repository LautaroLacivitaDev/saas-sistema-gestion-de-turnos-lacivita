# MEMORY.md

Memoria del proyecto: estado actual, decisiones tomadas y temas abiertos. Los agentes la leen al empezar y la actualizan al cerrar cada hito o al tomar una decisión. Las reglas de trabajo están en [AGENTS.md](AGENTS.md).

## Estado actual

- **Hito en curso:** Hito 1 (base del proyecto).
- **Último hito cerrado:** ninguno.
- **Repositorio:** https://github.com/LautaroLacivitaDev/saas-sistema-gestion-de-turnos-lacivita

## Hitos

| # | Hito | Estado |
|---|---|---|
| 1 | Base del proyecto: estructura, Docker Compose, Spring Boot con Modulith, Flyway, CI, documentación | Pendiente |
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
