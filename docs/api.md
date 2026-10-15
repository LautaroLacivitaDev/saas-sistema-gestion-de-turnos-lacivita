# Referencia de la API y de las pantallas

Cómo está organizada la API de Laciturnos, qué hace cada endpoint y quién puede usarlo. La documentación interactiva (Swagger UI) está en http://localhost:8080/api/docs con el backend corriendo en desarrollo; en producción está apagada.

Convenciones:

- La API usa **sesión en el servidor** (guardada en PostgreSQL) con la cookie `SESSION` (HttpOnly, SameSite=Lax). No hay JWT.
- Todo `POST`, `PUT` y `DELETE` exige el token **CSRF**: la cookie `XSRF-TOKEN` repetida en el encabezado `X-XSRF-TOKEN`.
- Los errores siguen **Problem Details** (RFC 9457) y traen un `code` estable (por ejemplo `slot_not_available`).
- Las horas de agenda van en la hora local de la sucursal (`HH:mm`); los instantes, en UTC (ISO 8601).
- Los listados son paginados (`page`, `size`).

## Autenticación

La API usa sesión en el servidor (guardada en PostgreSQL) con la cookie `SESSION` (HttpOnly). Los endpoints están en `/api/auth` y se pueden probar desde Swagger UI.

| Endpoint | Para qué |
|---|---|
| `GET /api/auth/csrf` | Entrega el token CSRF (también queda en la cookie `XSRF-TOKEN`). Todo `POST` lo exige en el encabezado `X-XSRF-TOKEN` |
| `POST /api/auth/register` | Crea la cuenta, inicia la sesión y envía el email de verificación |
| `POST /api/auth/login` | Inicia sesión con email y contraseña |
| `POST /api/auth/login-link` | Envía un link de acceso por email |
| `POST /api/auth/login-link/consume` | Inicia sesión con el token del link |
| `POST /api/auth/email-verification` | Verifica el email con el token del link |
| `POST /api/auth/email-verification/resend` | Reenvía el link de verificación |
| `GET /api/auth/me` | Devuelve la cuenta con sesión iniciada |
| `POST /api/auth/logout` | Cierra la sesión |

En desarrollo, los emails con los links se ven en Mailpit: http://localhost:8025.


Login con Google: ver [desarrollo.md](desarrollo.md#login-con-google).

## Negocios, sucursales y equipo

Todo exige sesión salvo la página pública. Los permisos dependen del rol en cada negocio (dueño, gerente o barbero).

| Endpoint | Para qué | Quién |
|---|---|---|
| `POST /api/businesses` | Crea un negocio; quien lo crea queda como dueño | Cualquier persona con sesión |
| `GET /api/businesses/slug-availability?slug=` | Verifica si un link está libre (mientras se escribe) | Cualquier persona con sesión |
| `GET /api/businesses/{id}` | Datos del negocio | Su equipo |
| `PUT /api/businesses/{id}/profile` | Cambia nombre, rubro y descripción | Dueño |
| `PUT /api/businesses/{id}/slug` | Cambia el link; el anterior sigue llevando al negocio | Dueño |
| `GET /api/businesses/{id}/branches` | Lista las sucursales | Su equipo |
| `POST /api/businesses/{id}/branches` · `PUT …/branches/{branchId}` | Crea o cambia una sucursal (con zona horaria) | Dueño |
| `GET /api/businesses/{id}/members` | Lista el equipo | Dueño y gerentes |
| `PUT …/members/{userId}/role` | Nombra o quita gerentes | Dueño |
| `PUT …/members/{userId}/branches` | Asigna sucursales | Dueño; gerente solo a barberos de sus sucursales |
| `DELETE …/members/{userId}` | Da de baja a un miembro (nunca al dueño) | Dueño; gerente solo a barberos |
| `POST` · `GET /api/businesses/{id}/invitations` · `DELETE …/invitations/{invitationId}` | Invita por email, lista o revoca invitaciones | Dueño (gerentes y barberos); gerente (barberos de sus sucursales) |
| `POST /api/invitations/accept` | Acepta una invitación con el token del email (tiene que ser el mismo email) | La persona invitada |
| `GET /api/memberships` | Negocios en los que trabaja la persona | Cualquier persona con sesión |
| `GET /api/businesses/{id}/audit-log` | Registro de cambios, incluidos los accesos de soporte | Dueño |
| `GET /api/public/businesses/{slug}` | Página pública con sus sucursales. Con un slug viejo, `canonicalSlug` indica el actual | Sin sesión |

Un `ADMIN` de la plataforma puede entrar a un negocio para dar soporte solo si manda el encabezado `X-Support-Reason` con el motivo. El acceso queda en el registro de cambios del negocio.

### Aislamiento entre negocios

Hay dos barreras independientes:

1. **Hibernate** filtra por `business_id` toda consulta sobre entidades de un negocio.
2. **Row Level Security en PostgreSQL**: la aplicación se conecta con el usuario `turnos_app`, que no puede saltearse las políticas. Las migraciones las corre otro usuario, dueño de las tablas.

El usuario `turnos_app` lo crea `docker/postgres/init/01-app-role.sql` la primera vez que se crea el volumen de Docker. Si tu base es anterior a ese script, crealo una vez a mano:

```bash
docker compose exec postgres psql -U turnos -d turnos -f /docker-entrypoint-initdb.d/01-app-role.sql
```

En producción se configuran por separado: `DATABASE_USERNAME` / `DATABASE_PASSWORD` (aplicación, sin privilegios) y `FLYWAY_USERNAME` / `FLYWAY_PASSWORD` (migraciones).

## Catálogo

Servicios del negocio (compartidos por todas las sucursales), combos y lo que hace cada profesional con su precio y su duración. Las rutas de la tabla cuelgan de `/api/businesses/{id}`, salvo la del catálogo público.

| Endpoint | Para qué | Quién |
|---|---|---|
| `GET /services` | Lista los servicios (filtro opcional `?status=ACTIVE`, `INACTIVE`, `PROPOSED` o `REJECTED`) | Todo el equipo |
| `POST /services` · `PUT /services/{serviceId}` | Agrega o edita un servicio: nombre, categoría, descripción, duración y precio base | Gerentes y dueño |
| `PUT /services/{serviceId}/price-range` | Fija el rango en el que cada barbero elige su precio (`min` y `max` vacíos lo quitan) | Dueño |
| `PUT /services/{serviceId}/active` | Retira o vuelve a ofrecer un servicio | Gerentes y dueño |
| `POST /service-proposals` | Propone un servicio nuevo, que queda esperando aprobación | Todo el equipo |
| `POST /services/{serviceId}/approve` · `…/reject` | Aprueba o rechaza una propuesta; quien la propuso pasa a ofrecer el servicio | Gerentes y dueño |
| `GET` · `POST /combos` · `PUT /combos/{comboId}` | Combos de 2 a 5 servicios que hace un mismo profesional; precio y duración se suman | Ver: todo el equipo. Crear y editar: gerentes y dueño |
| `GET /barbers/{userId}/services` | Lo que hace un profesional, con su precio y duración vigentes | Todo el equipo |
| `PUT /barbers/{userId}/services/{serviceId}` | Ofrece un servicio o cambia el precio y la duración propios (vacíos heredan los valores base) | Cada uno los suyos; gerente, los barberos de sus sucursales; dueño, todos |
| `DELETE /barbers/{userId}/services/{serviceId}` | Deja de ofrecer un servicio | Igual que la fila anterior |
| `GET /price-requests` · `POST /price-requests/{offeringId}/approve` · `…/reject` | Precios fuera de rango que pidieron los barberos | Gerentes (solo de sus sucursales) y dueño |
| `GET /api/public/businesses/{slug}/catalog` | Catálogo de la página pública: lo que se puede reservar, el precio de cada profesional y el "desde" | Sin sesión |

Si un barbero elige un precio fuera del rango, la respuesta trae `"outcome": "AWAITING_APPROVAL"` y sigue rigiendo su precio anterior hasta que un gerente lo apruebe. Los cambios de servicios y precios quedan en el registro de cambios del negocio.

## Agenda y disponibilidad

Horarios de atención de cada sucursal, horario de cada profesional en cada sucursal, feriados, bloqueos y el cálculo de horarios libres. Las horas van en la hora local de la sucursal (`HH:mm`); los instantes, en UTC (ISO 8601). Las rutas de la tabla cuelgan de `/api/businesses/{id}`, salvo las públicas.

| Endpoint | Para qué | Quién |
|---|---|---|
| `GET` · `PUT /branches/{branchId}/hours` | Horario de atención semanal de la sucursal. Se reemplaza entero; los días que no se mandan quedan cerrados | Ver: todos. Cambiar: gerentes de la sucursal y dueño |
| `GET /barbers/{userId}/schedule` | Horario de un profesional en cada una de sus sucursales | Todo el equipo |
| `PUT /barbers/{userId}/schedule/{branchId}` | Horario semanal del profesional en una sucursal. El descanso es el hueco entre dos franjas. No se puede superponer con su horario en otra sucursal (409 `schedule_overlap`) | Cada uno el suyo; gerente, los barberos de su sucursal; dueño, todos |
| `GET /holidays?from=&to=` · `POST /holidays` · `DELETE /holidays/{id}` | Feriados cargados a mano, de una sucursal o (sin `branchId`) de todo el negocio | Ver: todos. Sucursal: su gerente o el dueño. Todo el negocio: el dueño |
| `GET /time-blocks?from=&to=` | Bloqueos que se cruzan con el período | Todo el equipo |
| `POST /barbers/{userId}/time-blocks` | Bloquea a un profesional (trámite, vacaciones) | Cada uno los suyos; gerente, los barberos de sus sucursales; dueño, todos |
| `POST /branches/{branchId}/time-blocks` | Bloquea una sucursal entera (capacitación, refacción) | Gerentes de la sucursal y dueño |
| `DELETE /time-blocks/{id}` | Quita un bloqueo | Quien puede crearlo |
| `GET` · `PUT /schedule-rules` | Tiempo de preparación entre turnos, anticipación mínima (minutos) y máxima (días), cada cuántos minutos se ofrecen horarios | Ver: todos. Cambiar: el dueño |
| `GET /api/public/businesses/{slug}/branches/{branchId}/hours` | Horario de atención de la sucursal | Sin sesión |
| `GET /api/public/businesses/{slug}/availability?branchId=&date=&serviceId=` (o `comboId=`, y opcional `barberId=`) | Horarios libres de un día. Sin `barberId` es "cualquiera disponible": cada horario trae los profesionales libres con su precio y su duración | Sin sesión |

Un horario se ofrece si el profesional trabaja y la sucursal atiende, no es feriado, no hay un bloqueo ni un turno (más el tiempo de preparación) y respeta la anticipación. Los horarios se calculan en la zona de la sucursal, incluidos los cambios de horario de verano.

## Reservas

### Reserva online (sin cuenta)

En tres pasos, desde la página pública del negocio (`/api/public/businesses/{slug}`):

| Endpoint | Para qué |
|---|---|
| `POST /holds` | Reserva el horario 5 minutos (`serviceId` o `comboId`, `branchId`, `startsAt` y opcional `barberId`; sin profesional, se asigna al libre con menos turnos ese día). 409 `slot_not_available` si ya no está libre |
| `POST /holds/{holdId}/guest-code` | Invitados: guarda nombre, email y teléfono, verifica con Cloudflare Turnstile (`humanToken`) y manda un código de 6 dígitos por email |
| `POST /holds/{holdId}/confirm` | Confirma con el `code` del email (5 intentos). Con sesión y email verificado no hace falta código. Llega un email de confirmación con el `.ics` y los botones para gestionar el turno |

Los tres llevan límite de intentos por IP. El precio y la duración de cada servicio se copian en el turno: si después cambian, el turno no cambia.

### Link del cliente

Sin sesión, con el token del link del email (va en el cuerpo, no en la URL). Cada email trae su propio link y todos siguen funcionando. Los botones del email abren `/turno?token=…&accion=confirmar|reprogramar|cancelar` en el frontend.

| Endpoint | Para qué |
|---|---|
| `POST /api/public/appointments/lookup` | Muestra el turno y hasta cuándo se puede cambiar (`changeableUntil`) |
| `POST /api/public/appointments/confirm` | El cliente confirma que va a un turno que el local cargó "a confirmar" |
| `POST /api/public/appointments/cancel` | Cancela, dentro del plazo del negocio (422 `change_deadline_passed`) |
| `POST /api/public/appointments/reschedule` | Pasa el turno a otro horario libre del mismo profesional, dentro del plazo |

### Agenda del equipo

Rutas bajo `/api/businesses/{id}`. Cada persona trabaja con los turnos de sus sucursales (el dueño, de todas). El contacto del cliente lo ven el dueño, los gerentes y el profesional del turno.

| Endpoint | Para qué | Quién |
|---|---|---|
| `GET /appointments?from=&to=` (opcional `branchId`, `barberId`) | Turnos del período (hasta un mes) | Todo el equipo, en su alcance |
| `POST /appointments` | Carga un turno de un cliente que llamó o llegó sin reserva (`confirmed: false` lo deja a confirmar) | Todo el equipo, en sus sucursales |
| `PUT /appointments/{id}/status` | `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `NO_SHOW` o `CANCELLED`, según el estado actual | Todo el equipo, en sus sucursales |
| `PUT /appointments/{id}/time` | Mueve el turno a otro horario y, si se indica, a otro profesional | Todo el equipo, en sus sucursales |
| `GET` · `PUT /booking-settings` | Plazo para que el cliente cancele o reprograme (horas antes; 2 por defecto) | Ver: todos. Cambiar: el dueño |

Dos turnos activos del mismo profesional nunca se superponen: lo impide una restricción de exclusión en PostgreSQL, aunque dos personas reserven el mismo horario a la vez. Las escrituras sobre la agenda de un profesional se ordenan con un bloqueo por profesional, así muchas reservas simultáneas del mismo horario reciben un 409 claro en lugar de errores.
## Sitio público

Pensado para el celular: una columna, botones de 44 px, letra de 16 px en los formularios y la acción principal fija al pie. Probado a 320 y 375 px y en escritorio.

| Página | Qué hay |
|---|---|
| `/` y `/buscar?q=` | Buscador de negocios por nombre, rubro o barrio, que tolera errores de tipeo y tildes ("barbria palerno" encuentra "Barbería El Tano", en Palermo) |
| `/{slug}` | Página del negocio, armada en el servidor: servicios con precio "desde", combos, profesionales, sucursales con horarios, "Cómo llegar" y el QR para compartir. Open Graph y datos estructurados (schema.org). Un link viejo redirige al actual |
| `/{slug}/profesionales/{id}` | Perfil del profesional: foto, descripción, especialidades, precios y "Reservar con…" |
| `/{slug}/reservar` | Reserva en 3 pasos: servicio, día y horario (el horario queda guardado 5 minutos), y datos con código por email (o confirmar directo con cuenta y email verificado). Acepta lo ya elegido en el link: `?sucursal=&servicio=` o `combo=` y `profesional=` |
| `/turno?token=` | El link de los emails: ver el turno, confirmar que va, cambiar día u horario o cancelar, dentro del plazo. `&accion=` abre directo la acción |
| `/mis-turnos` | Con cuenta: próximos y anteriores en todos los negocios, "Repetir mi última reserva" y "Reservar de nuevo" |
| `/ingresar`, `/registro`, `/verificar-email`, `/acceso` | Cuenta: ingresar con contraseña o con un link por email, crear cuenta y los links de los emails |

Endpoints públicos del sitio:

| Endpoint | Para qué | Quién |
|---|---|---|
| `GET /api/public/businesses?q=&page=&size=` | Buscador (páginas de 20; `hasMore` dice si hay más) | Público |
| `GET /api/public/businesses/{slug}/professionals` y `/{id}` | Profesionales con perfil, sucursales y precios | Público |
| `GET` · `PUT /api/businesses/{id}/barbers/{barberId}/profile` | Perfil público: descripción, hasta 6 especialidades y la foto como link https | El profesional, su gerente o el dueño |

## Panel del negocio

En `/panel`, para el equipo. Barra inferior en el celular y navegación lateral desde tablet; cada rol ve solo lo que puede hacer. Probado a 320 y 375 px y en escritorio.

| Página | Qué hay | Quién |
|---|---|---|
| `/panel/nuevo` | Alta guiada en 6 pasos: negocio y link, sucursal, horario, servicios, "yo atiendo" y equipo | Con cuenta |
| `…/agenda` | Turnos por día o semana (Día por defecto en el celular), por sucursal y profesional; ficha del turno con sus acciones y "Nuevo turno" de mostrador | Todo el equipo |
| `…/clientes` | Base de clientes: búsqueda por nombre, email o teléfono, ficha con historial, notas y preferencias | Gerente y dueño |
| `…/catalogo` | Servicios, combos, rango de precios y aprobaciones (servicios propuestos y precios fuera de rango) | Todo el equipo (el barbero propone) |
| `…/equipo` | Miembros con rol y sucursales, bajas, invitaciones pendientes. La invitación se acepta en `/invitacion?token=` | Gerente y dueño |
| `…/sucursales` | Datos, horario de atención, días cerrados y cierres por horas de cada sucursal | Gerente (las suyas) y dueño |
| `…/horarios` | Horario de cada profesional por sucursal y sus bloqueos; las reglas de la agenda, solo el dueño | Todo el equipo |
| `…/perfil` | Perfil público y mis servicios con precio y duración propios | Todo el equipo |
| `…/avisos` | Avisos de turnos nuevos, movidos y cancelados; marcar como leídos | Todo el equipo |
| `…/ajustes` | Datos y link del negocio, plazo de cancelación, recordatorios y textos de los emails | Dueño |

`GET /api/memberships` incluye `branchIds` (vacío para el dueño, que trabaja en todas).

### Mis turnos

| Endpoint | Para qué | Quién |
|---|---|---|
| `GET /api/me/appointments` | Mis últimos 50 turnos como cliente, en todos los negocios | Con sesión |
| `POST /api/me/appointments/{id}/manage-link` | Abre la gestión de uno de mis turnos: devuelve un token de gestión nuevo (el mismo que llega por email) para cambiarlo o cancelarlo en `/turno?token=`. 404 si el turno no es de la cuenta | Con sesión |

## Notificaciones

Cada cambio en un turno deja sus avisos en una bandeja de salida (tabla `notification`) en la misma transacción: si el turno se guarda, el aviso también, y no se pierde aunque el envío falle.

| Cambio | Cliente | Profesional |
|---|---|---|
| Turno reservado | Email con `.ics` y botones (confirmar si está "a confirmar", cambiar, cancelar) | Aviso en la app y email |
| Recordatorio (24 y 2 horas antes, configurable) | Email con botones | Resumen de su agenda a las 7:00 del día |
| Turno movido | Email con `.ics` nuevo; los recordatorios se reprograman | Aviso en la app y email (también a quien lo tenía antes) |
| Turno cancelado | Email con `.ics` cancelado; los recordatorios se cancelan | Aviso en la app y email |

A quien hizo el cambio no se le avisa. Los emails salen apenas se confirma el cambio; una tarea de JobRunr revisa cada minuto los recordatorios, los resúmenes y los reintentos (5 intentos: al momento y a los 1, 5, 15 y 60 minutos). En desarrollo, todos los emails llegan a Mailpit (http://localhost:8025).

Rutas bajo `/api/businesses/{id}`:

| Endpoint | Para qué | Quién |
|---|---|---|
| `GET /appointments/{id}/notifications` | Registro de envíos del turno: `PENDING`, `SENT`, `FAILED`, `CANCELLED` o `SKIPPED` (por ejemplo, el cliente no dejó email) | Quien ve el turno |
| `GET /notices` · `GET /notices/unread-count` | Mis avisos en la app (paginados) y cuántos no leí | Todo el equipo |
| `POST /notices/{id}/read` · `POST /notices/read-all` | Marca avisos como leídos | Cada uno, los suyos |
| `GET` · `PUT /notification-settings` | Horas antes del turno en que se recuerda (`reminderHours`, hasta 3; vacío los desactiva) | Ver: todos. Cambiar: el dueño |
| `GET /message-templates` | Textos de los emails al cliente y las variables disponibles | Gerentes y dueño |
| `PUT` · `DELETE /message-templates/{type}` | Personaliza el asunto y el mensaje, o vuelve al texto de Laciturnos | El dueño |

Los textos aceptan las variables `{nombre}`, `{servicio}`, `{barbero}`, `{sucursal}`, `{direccion}`, `{hora}` y `{negocio}`. El diseño del email, los datos del turno y los botones son fijos: el negocio escribe texto, no HTML.

