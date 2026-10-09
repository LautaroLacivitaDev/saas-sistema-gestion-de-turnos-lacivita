# Laciturnos: gestión de turnos para barberías y estéticas

Plataforma SaaS para que barberías, centros de estética y negocios similares administren su agenda y reciban reservas online. Cada negocio tiene su página pública de reservas en `/{nombre-del-local}`.

- Especificación del MVP: [docs/especificacion-mvp.md](docs/especificacion-mvp.md)
- Estado del proyecto y decisiones: [MEMORY.md](MEMORY.md)
- Instrucciones para agentes de IA: [AGENTS.md](AGENTS.md)

## Requisitos

| Herramienta | Versión |
|---|---|
| JDK | 25 (Eclipse Temurin recomendado) |
| Node.js | 24 |
| Docker Desktop | Con WSL 2 en Windows. Tiene que estar corriendo para el entorno local y para las pruebas del backend |
| Git | Cualquier versión reciente |

## Puesta en marcha

### 1. Servicios locales

```bash
docker compose up -d
```

| Servicio | Dirección |
|---|---|
| PostgreSQL 17 | `localhost:5433`, base `turnos`. Usuario `turnos` (dueño, para migraciones) y `turnos_app` (la aplicación). Contraseña igual al usuario |
| Mailpit (emails de prueba) | SMTP en `localhost:1025`, interfaz web en http://localhost:8025 |

PostgreSQL usa el puerto **5433** para no chocar con una instalación local en el 5432.

### 2. Backend

```bash
cd backend
mvnw.cmd spring-boot:run     # Windows
./mvnw spring-boot:run       # macOS / Linux
```

- API: http://localhost:8080/api
- Documentación de la API (Swagger UI): http://localhost:8080/api/docs
- Estado: http://localhost:8080/actuator/health

Sin perfil explícito arranca con `dev`. Las migraciones de Flyway se aplican solas al iniciar.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Abrí http://localhost:3000. Las llamadas a `/api/*` se redirigen al backend en `localhost:8080`. Para cambiar esa dirección, copiá `.env.example` como `.env.local`.

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

### Login con Google

Está desactivado hasta que configures las credenciales:

1. En [Google Cloud Console](https://console.cloud.google.com/apis/credentials), creá un **ID de cliente de OAuth** de tipo *Aplicación web*.
2. Como **URI de redireccionamiento autorizado** cargá `http://localhost:3000/api/auth/oauth2/code/google`. Pasa por el frontend para que la cookie de sesión quede en el mismo dominio.
3. Levantá el backend con el perfil `google` y las credenciales como variables de entorno:

   ```powershell
   $env:SPRING_PROFILES_ACTIVE = "dev,google"
   $env:GOOGLE_CLIENT_ID = "..."
   $env:GOOGLE_CLIENT_SECRET = "..."
   cd backend; .\mvnw.cmd spring-boot:run
   ```

4. El login empieza en http://localhost:3000/api/auth/oauth2/authorization/google.

Nunca subas las credenciales al repositorio.

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
| `POST /holds/{holdId}/confirm` | Confirma con el `code` del email (5 intentos). Con sesión y email verificado no hace falta código. Llega por email el link para gestionar el turno |

Los tres llevan límite de intentos por IP. El precio y la duración de cada servicio se copian en el turno: si después cambian, el turno no cambia.

### Link del cliente

Sin sesión, con el token del link del email (va en el cuerpo, no en la URL):

| Endpoint | Para qué |
|---|---|
| `POST /api/public/appointments/lookup` | Muestra el turno y hasta cuándo se puede cambiar (`changeableUntil`) |
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

Dos turnos activos del mismo profesional nunca se superponen: lo impide una restricción de exclusión en PostgreSQL, aunque dos personas reserven el mismo horario a la vez.

### Cloudflare Turnstile

En desarrollo y en las pruebas está desactivado (la aplicación lo avisa en el log). En producción es obligatorio: configurá `TURNSTILE_SECRET_KEY` con la clave secreta del sitio; sin ella la aplicación no arranca.

## Pruebas

```bash
# Backend: formato, compilación y todas las pruebas (incluye PostgreSQL real con Testcontainers)
cd backend
mvnw.cmd verify              # Windows
./mvnw verify                # macOS / Linux

# Frontend
cd frontend
npm run lint
npm run typecheck
npm test
npm run build
```

Si `verify` falla por formato, corré `mvnw.cmd spotless:apply` (o `./mvnw spotless:apply`) y volvé a probar.

## IDE

### IntelliJ IDEA (backend)

1. **File → Open** y elegí la carpeta raíz del repositorio. IntelliJ importa el backend como proyecto Maven automáticamente.
2. En **File → Project Structure → SDK** elegí el JDK 25.
3. Las configuraciones de ejecución ya vienen incluidas (carpeta `.run/`): *Backend (dev)*, *Backend - verify*, *Backend - formatear*, *Docker Compose - levantar* y *Frontend (dev)*.
4. Para que el formato de IntelliJ coincida con Spotless, instalá el plugin **palantir-java-format** desde el Marketplace y activalo en **Settings → palantir-java-format**.

Funciona con IntelliJ IDEA Community.

### VS Code (frontend)

Abrí la carpeta `frontend/`. VS Code te va a sugerir las extensiones recomendadas (ESLint, Tailwind CSS, Vitest y Claude Code).

## Estructura

```
/
├── backend/             # Spring Boot 4.1, monolito modular (Spring Modulith)
├── frontend/            # Next.js 16, TypeScript, Tailwind CSS, shadcn/ui
├── docs/                # Especificación
├── .github/workflows/   # CI: build y pruebas en cada push
├── .run/                # Configuraciones de ejecución de IntelliJ
└── docker-compose.yml   # PostgreSQL y Mailpit para desarrollo
```
