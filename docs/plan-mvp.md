# Plan del MVP (Fase 1)

Plan aprobado el 2026-10-08. El estado de cada hito se actualiza al cerrarlo; el detalle de lo resuelto queda en [decisiones.md](decisiones.md).

## Hitos

| # | Hito | Criterios de aceptación | Estado |
|---|---|---|---|
| 1 | **Base del proyecto** | Estructura `backend/` y `frontend/`. Docker Compose con PostgreSQL y Mailpit. `verify` pasa con pruebas de Modulith y Testcontainers. Flyway desde cero. Spotless, CI, `.run/`, `.editorconfig`, documentación. Next.js con Tailwind y shadcn/ui. | Terminado (2026-10-08) |
| 2 | **Usuarios y autenticación** | Registro y login con email y contraseña, login con Google con vinculación por email verificado, sesión con cookie HttpOnly y CSRF, link de acceso por email, `PermissionEvaluator` con pruebas por rol, límite de intentos con Bucket4j. | Terminado (2026-10-08). Google probado con proveedor simulado |
| 3 | **Negocios y sucursales** | Alta de negocio con slug validado, palabras reservadas y redirección al cambiarlo. Sucursales con zona horaria. Invitaciones (el dueño nombra gerentes, el gerente invita barberos). RLS activo con prueba de aislamiento entre negocios. Auditoría básica. | Terminado (2026-10-08) |
| 4 | **Catálogo** | Servicios, combos y servicios por barbero con precio y duración propios o heredados. Rango de precios del dueño y aprobación del gerente fuera de rango. | Terminado (2026-10-08) |
| 5 | **Agenda y disponibilidad** | Horarios por barbero y sucursal sin superposición, descansos, bloqueos, feriados y tiempo de preparación. Cálculo de disponibilidad probado con cambios de horario. Restricción de exclusión en los horarios con prueba de guardados simultáneos. | Terminado (2026-10-08) |
| 6 | **Reservas** | Restricción de exclusión en los turnos y prueba de reservas simultáneas. Flujo con bloqueo `HOLD`, "cualquiera disponible", reserva como invitado con código por email y Turnstile, estados, cancelación y reprogramación por link firmado, carga manual y copia del precio al reservar. | Terminado (2026-10-08) |
| 7 | **Notificaciones** | Outbox transaccional, envío con reintentos (JobRunr), recordatorios que se reprograman, plantillas Thymeleaf editables, `.ics`, registro de envíos y avisos en la app. | Terminado (2026-10-09) |
| 8 | **Frontend público** | Buscador tolerante a errores y tildes, página `/{slug}` con renderizado en servidor, perfil público de cada profesional (foto, descripción y especialidades), Open Graph, datos estructurados y QR, flujo de reserva mobile first (probado a 320 y 375 px, acción principal al alcance del pulgar), historial y "repetir última reserva". | Terminado (2026-10-09) |
| 9 | **Frontend del panel** | Panel usable desde el celular: agenda con selector "Día / Semana" (por defecto Día en el teléfono y Semana desde tablet), por profesional y sucursal; listas o tarjetas en lugar de tablas en pantalla chica. Gestión de catálogo, equipo, sucursales, horarios y clientes. Onboarding guiado. | Terminado (2026-10-09) |
| 10 | **Endurecimiento y publicación** | Playwright de punta a punta en viewport de celular y de escritorio, k6 sobre el mismo horario, observabilidad, revisión de seguridad y aislamiento, PWA. Publicación como proyecto de portfolio en subdominios gratuitos del hosting, con modo demo (los emails se ven en la app en lugar de enviarse) y datos de ejemplo. | Pendiente |

La app se usa sobre todo desde el celular: un hito con pantallas se cierra recién después de probarlas en celular (320 y 375 px) y en escritorio. Las reglas están en [AGENTS.md](../AGENTS.md), sección "Mobile first".

## Riesgos identificados

- Compatibilidad de librerías con Spring Boot 4.1 (springdoc, JobRunr, Modulith, Bucket4j). Resuelto: JobRunr desde el Hito 7 (8.8.2).
- RLS con pool de conexiones: el negocio de la transacción mal configurado mezclaría datos. Resuelto en el Hito 3: el contexto se escribe al entregar cada conexión y hay pruebas de aislamiento.
- "Próximo turno disponible" en el buscador es caro: calcularlo solo para los primeros resultados y cachearlo.
- Zonas horarias y cambios de horario: probar con una zona que cambie la hora.
- Alcance grande: los Hitos 1 a 7 dejan una API completa probable desde Swagger antes del frontend.
