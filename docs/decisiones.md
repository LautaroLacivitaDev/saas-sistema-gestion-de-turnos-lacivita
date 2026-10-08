# Registro de decisiones

Todas las decisiones del proyecto con su motivo, en orden cronológico. [MEMORY.md](../MEMORY.md) resume las vigentes más importantes. En el código, las decisiones puntuales se marcan con `// DECISIÓN:`.

## Producto (2026-10-07)

| Decisión | Por qué |
|---|---|
| WhatsApp fuera del proyecto hasta terminar el desarrollo. Notificaciones solo por email, con una capa de canales | Reducir alcance. La capa de canales permite sumarlo sin reescribir |
| Sin rol de recepción, ni siquiera previsto en el modelo | No se va a usar en el corto plazo |
| Solo el dueño nombra o quita gerentes. El gerente invita barberos (reciben el rol al aceptar) y los da de baja | Resolvía una contradicción entre la matriz de permisos y las reglas de delegación |
| El barbero siempre ve su propia agenda. Ver el resto de la sucursal es configurable por el dueño (activado por defecto) | Pedido explícito |

## Plan del MVP (2026-10-08)

| Decisión | Por qué |
|---|---|
| Paquete base `com.lacivita.turnos` | Preferencia del dueño del proyecto |
| Roles en dos niveles: Spring Security maneja `ADMIN` y usuario común; `OWNER`, `MANAGER` y `BARBER` salen de la membresía en cada negocio y los resuelve el `PermissionEvaluator` | Una persona puede ser dueña de un negocio y cliente de otro. Difiere de la especificación (que pedía `RoleHierarchy` para todo) y fue aprobado |
| Login con Google después: primero email y contraseña, pruebas con proveedor simulado | Faltan las credenciales OAuth de Google Cloud Console |
| Panel del dueño en el MVP: turnos por estado y ocupación del día por sucursal | Los reportes de ingresos son de la Fase 2 |
| Feriados cargados a mano por sucursal | Más simple que integrar una API de feriados |
| Servicios combinados con un solo profesional | Dos profesionales complica la disponibilidad y no es necesario en el MVP |
| Bloqueo temporal: turno `HOLD` que vence a los 5 minutos | La misma restricción de exclusión protege la reserva en curso |
| Barbero = membresía con perfil, por negocio | Reutiliza la membresía y evita duplicar datos de la persona |
| Disponibilidad con SQL nativo (`JdbcClient`) en lugar de jOOQ | Evita configurar el generador de código de jOOQ |
| Auditoría en tabla propia (`audit_log`) alimentada por eventos | Más simple de consultar que Envers y no ata los módulos |
| Estado de emails `ENVIADO` o `FALLIDO` | `ENTREGADO` necesita webhooks del proveedor |
| Avisos en la app con consulta periódica | WebSockets no se justifican en el MVP |
| En desarrollo, Next.js redirige `/api/*` al backend | Un solo dominio: la cookie de sesión funciona sin CORS |

## Hito 1 (2026-10-08)

| Decisión | Por qué |
|---|---|
| Spring Boot 4.1.1, Modulith 2.1.1, springdoc 3.1.1, Next.js 16.4, React 19.3, Tailwind 4, Vitest 5, PostgreSQL 17.11 | Últimas versiones estables y compatibles entre sí |
| PostgreSQL de Docker en el puerto 5433 | La PC de desarrollo ya tiene PostgreSQL en el 5432 |
| Tabla `event_publication` de Modulith creada con Flyway (`V2`) | Esquema versionado; Hibernate valida con `ddl-auto: validate` |
| POM agregador en la raíz | Que IntelliJ importe el backend al abrir la carpeta. El build real usa el wrapper de `backend/` |
| Configuraciones `.run/` del frontend y Docker de tipo *Shell Script* | IntelliJ Community no tiene configuraciones de npm |
| Vitest sin `@vitejs/plugin-react` | Choca con Babel 7 de Next; Vite 8 ya compila JSX y resuelve alias |
| Spotless con palantir-java-format ligado a `verify` | Formato uniforme; el CI falla si no se respeta |
| CI sin SonarCloud por ahora | Necesita un token de la cuenta |
| Swagger UI en `/api/docs`, OpenAPI en `/api/openapi`, apagados en producción | No exponer la API documentada en producción |
| Perfil por defecto `dev` | El backend arranca desde el IDE sin configurar nada |
