# Plan del MVP (Fase 1)

Plan aprobado el 2026-10-08. El estado de cada hito se actualiza al cerrarlo; el detalle de lo resuelto queda en [decisiones.md](decisiones.md).

## Hitos

| # | Hito | Criterios de aceptación | Estado |
|---|---|---|---|
| 1 | **Base del proyecto** | Estructura `backend/` y `frontend/`. Docker Compose con PostgreSQL y Mailpit. `verify` pasa con pruebas de Modulith y Testcontainers. Flyway desde cero. Spotless, CI, `.run/`, `.editorconfig`, documentación. Next.js con Tailwind y shadcn/ui. | Terminado (2026-10-08) |
| 2 | **Usuarios y autenticación** | Registro y login con email y contraseña, login con Google con vinculación por email verificado, sesión con cookie HttpOnly y CSRF, link de acceso por email, `PermissionEvaluator` con pruebas por rol, límite de intentos con Bucket4j. | Pendiente |
| 3 | **Negocios y sucursales** | Alta de negocio con slug validado, palabras reservadas y redirección al cambiarlo. Sucursales con zona horaria. Invitaciones (el dueño nombra gerentes, el gerente invita barberos). RLS activo con prueba de aislamiento entre negocios. Auditoría básica. | Pendiente |
| 4 | **Catálogo** | Servicios, combos y servicios por barbero con precio y duración propios o heredados. Rango de precios del dueño y aprobación del gerente fuera de rango. | Pendiente |
| 5 | **Agenda y disponibilidad** | Horarios por barbero y sucursal sin superposición, descansos, bloqueos, feriados y tiempo de preparación. Cálculo de disponibilidad probado con cambios de horario. Restricción de exclusión y prueba de reservas simultáneas. | Pendiente |
| 6 | **Reservas** | Flujo con bloqueo `HOLD`, "cualquiera disponible", reserva como invitado con código por email y Turnstile, estados, cancelación y reprogramación por link firmado, carga manual y copia del precio al reservar. | Pendiente |
| 7 | **Notificaciones** | Outbox transaccional, envío con reintentos (JobRunr), recordatorios que se reprograman, plantillas Thymeleaf editables, `.ics`, registro de envíos y avisos en la app. | Pendiente |
| 8 | **Frontend público** | Buscador tolerante a errores y tildes, página `/{slug}` con renderizado en servidor, Open Graph, datos estructurados y QR, flujo de reserva mobile first, historial y "repetir última reserva". | Pendiente |
| 9 | **Frontend del panel** | Agenda por día y semana, por profesional y sucursal. Gestión de catálogo, equipo, sucursales, horarios y clientes. Onboarding guiado. | Pendiente |
| 10 | **Endurecimiento** | Playwright de punta a punta, k6 sobre el mismo horario, observabilidad, revisión de seguridad y aislamiento, PWA. | Pendiente |

## Riesgos identificados

- Compatibilidad de librerías con Spring Boot 4.1 (springdoc, JobRunr, Modulith, Bucket4j). Resuelto para las del Hito 1.
- RLS con pool de conexiones: el negocio de la transacción mal configurado mezclaría datos. Se cubre con pruebas en el Hito 3.
- "Próximo turno disponible" en el buscador es caro: calcularlo solo para los primeros resultados y cachearlo.
- Zonas horarias y cambios de horario: probar con una zona que cambie la hora.
- Alcance grande: los Hitos 1 a 7 dejan una API completa probable desde Swagger antes del frontend.
