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

## Hito 2 (2026-10-08)

| Decisión | Por qué |
|---|---|
| Sesiones en PostgreSQL con Spring Session JDBC (cookie `SESSION`, HttpOnly, SameSite=Lax, 14 días) | El servidor no guarda estado en memoria y se pueden sumar instancias. Sin JWT en el navegador |
| Cookie configurada con un bean `CookieSerializer` | En Boot 4 las propiedades `server.servlet.session.cookie.*` no llegan a Spring Session: la cookie salía sin HttpOnly |
| CSRF en modo SPA: cookie `XSRF-TOKEN` y encabezado `X-XSRF-TOKEN`; `GET /api/auth/csrf` lo entrega | Patrón estándar de Spring Security para frontends que llaman a la API |
| Un solo principal (`AuthenticatedUser`) para contraseña, link de acceso y Google, guardado por `SessionAuthenticator` | El resto del código trabaja con un solo tipo, sin importar cómo entró la persona |
| `shared.security` define `BusinessRoleResolver` y `ExternalIdentityResolver`; `users` los implementa | La seguridad no depende del módulo de usuarios: respeta los límites de Modulith |
| Login con contraseña con hash señuelo cuando el email no existe | La respuesta tarda lo mismo y no revela qué emails están registrados |
| Registro con email existente responde 409 | Mejor experiencia; el límite por IP acota que se use para averiguar emails |
| Link de acceso responde 202 siempre | No revela si el email tiene cuenta |
| Usar un link de acceso verifica el email | Prueba que la persona controla el email |
| Google sobre una cuenta sin verificar: vincula, verifica y descarta la contraseña | Quien registró un email ajeno no conserva el acceso (pre-account takeover) |
| Solo se acepta Google con `email_verified` | Sin eso cualquiera podría declarar el email de otra persona |
| Tokens de email: 256 bits aleatorios, se guarda solo el SHA-256, un uso, 24 h (verificación) o 15 min (acceso) | Quien lea la base no puede usar los tokens |
| Emails de cuenta después del commit, en segundo plano y sin reintentos | Si fallan, se piden de nuevo. Los eventos persistidos de Modulith guardarían el token en claro |
| Emails de texto plano | Las plantillas editables llegan con notificaciones (Hito 7) |
| Rate limit por IP (10 por minuto) en memoria con Caffeine y Bucket4j | Suficiente con una instancia; se pasa a almacenamiento compartido en el Hito 10 si hace falta |
| Login con Google en el perfil `google`, con redirección a través del frontend | Sin credenciales la app arranca igual; la cookie queda en el dominio del frontend |
| Membresía con `business_id` sin clave foránea todavía | La tabla `business` llega en el Hito 3, que agrega la FK |
| El acceso de un ADMIN a un negocio todavía no se audita | La auditoría llega en el Hito 3; el `PermissionEvaluator` es el punto donde se va a registrar |
| Repositorios: interfaces de Spring Data en el dominio que extienden `Repository` con solo los métodos necesarios | Evita una capa de adaptadores que solo delegaría |
| Ids UUID v7 generados por la entidad | Inserción ordenada en índices y entidades válidas desde que se crean |
| Health de mail desactivado | Una caída del proveedor de email no debe marcar la API como caída |

## Revisión de código del Hito 2 (2026-10-08)

| Decisión | Por qué |
|---|---|
| Capas por módulo: `web → application → domain ← infrastructure`, verificadas con ArchUnit | El orden de capas queda garantizado por el build, no solo por convención |
| Puerto `PasswordHasher` en el dominio; `User` verifica su propia contraseña y no expone el hash | Encapsulamiento: la regla vive en la entidad y el algoritmo es un detalle de infraestructura |
| Los módulos declaran sus endpoints públicos con `PublicEndpoints` | `shared` no conoce las rutas de los módulos; sumar un endpoint público no toca la seguridad común |
| La capa web responde con sus propios DTOs (`AuthResponses`) | El contrato HTTP no queda atado a los modelos de la aplicación |
| `UserRepository.require(id)` para ids que vienen de una sesión o de un token | Una sola forma de tratar la inconsistencia, sin repetir el `orElseThrow` |
| El registro solo traduce a "email ya registrado" la violación de `user_account_email_uk` | Otras violaciones de integridad no se disfrazan de un error de negocio |
| Se quitaron getters sin uso y el atributo `phone` sin escritura; getters con el mismo estilo en todas las entidades | Sin código muerto; consistencia |

## Hito 3: negocios, sucursales y equipo (2026-10-08)

| Decisión | Por qué |
|---|---|
| El nombre de la app es **Laciturnos** (lo eligió Lautaro). Se usa en la interfaz, los emails y la documentación; el paquete `com.lacivita.turnos`, la base `turnos` y los contenedores no cambian | Renombrar lo interno no aporta nada y obligaría a recrear la base local |
| Slug de 3 a 50 caracteres (minúsculas, números y guiones), con palabras reservadas (rutas de la app como `api`, `admin`, `login`) | Evita que un negocio tape una página de la plataforma |
| Todo slug usado queda reservado para siempre al negocio (`business_slug`) y el viejo sigue llevando al actual | Los links compartidos en redes no se rompen ni llevan a otro negocio |
| La API responde el slug canónico y redirige el frontend, en lugar de un 301 | Next.js sigue los redirects de `fetch` en silencio y el navegador no vería la URL nueva |
| Zona horaria por sucursal, `America/Argentina/Buenos_Aires` por defecto | Los turnos se guardan en UTC y se muestran según la sucursal |
| Invitaciones por email válidas 7 días; invitar de nuevo al mismo email reemplaza el link anterior; se aceptan solo con una cuenta del mismo email, y aceptar verifica el email | El link llegó a ese email: usarlo prueba que la persona lo controla |
| Reglas del equipo en `TeamPolicy` (dominio): nadie toca al dueño, solo el dueño nombra o quita gerentes, el gerente gestiona barberos solo de sus sucursales | Las reglas se prueban sin Spring y el servicio solo orquesta |
| Aislamiento en dos barreras: `@TenantId` de Hibernate y Row Level Security en PostgreSQL | Si una falla, la otra sigue protegiendo; RLS cubre también el SQL escrito a mano |
| La app se conecta con `turnos_app` (sin superusuario ni `BYPASSRLS`); Flyway con el dueño de las tablas | Un superusuario se saltea RLS sin avisar |
| El negocio se fija con `@BusinessScoped` + `@BusinessId` antes de abrir la transacción | La conexión toma el contexto al obtenerse; fijarlo adentro llegaría tarde |
| La persona con sesión se fija en `TenantContext` desde un filtro de seguridad (`SignedInUserScopeFilter`); la conexión nunca lee la seguridad | Leer la seguridad al pedir una conexión cargaba la sesión desde PostgreSQL, que pedía otra conexión, en cadena, hasta agotar el pool |
| Los permisos leen la membresía dentro del negocio consultado (`MembershipRoles` es `@BusinessScoped`) | Funcionan igual dentro y fuera de una solicitud HTTP |
| Operaciones que cruzan negocios solo con `TenantContext.callAsSystem(motivo, …)` (hoy: buscar a qué negocio pertenece el token de una invitación) | Quedan pocas, explícitas y con su porqué en el código |
| Auditoría síncrona en la misma transacción (`audit_log`), con antes y después en JSON | Si el cambio se guarda, el registro también; nunca uno sin el otro |
| El acceso de un ADMIN a un negocio exige el encabezado `X-Support-Reason`, se audita una vez por solicitud y lo ve el dueño | Soporte transparente: el dueño sabe quién entró y por qué |
| `@Version` como `Long` (nulo hasta el primer guardado) en las entidades con id propio | Con `long` Spring Data creía que un negocio nuevo ya existía y hacía `merge` en vez de `persist` |
| Las pruebas mandan el CSRF como el navegador (`SpaCsrf`: cookie más encabezado) en lugar de `csrf()` de Spring Security Test | `csrf()` reemplaza el repositorio CSRF del contexto compartido y rompía otras pruebas según el orden |

## Mobile first (2026-10-08)

| Decisión | Por qué |
|---|---|
| La app se diseña primero para el celular (360 px) y escala a tablet y escritorio. Reglas en AGENTS.md, sección "Mobile first" | Lautaro: la mayoría de clientes, barberos y dueños la van a usar desde el teléfono |
| Toda pantalla nueva se prueba a 320 y 375 px y en escritorio antes de cerrar un hito | Es parte de la definición de "terminado", no un retoque al final |
| Viewport explícito con `viewport-fit=cover`, zonas seguras del notch con `env(safe-area-inset-*)`, alturas con `dvh` y sin bloquear el zoom | Aprovecha toda la pantalla en celulares con notch sin perder accesibilidad |
| Se corrigió `--font-sans`, que el tema de shadcn dejaba apuntando a sí mismo (la app salía en Times) | Se vio al revisar la pantalla en tamaño celular |
| Agenda del panel con selector "Día / Semana" en cualquier pantalla: por defecto Día en el celular (con flechas y tira de la semana) y Semana desde tablet. Lo eligió Lautaro | Siete columnas en 375 px no se leen ni se tocan bien; el selector deja ver la semana a quien la necesite |
