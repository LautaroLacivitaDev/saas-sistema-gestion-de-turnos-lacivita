# Proyecto: Sistema de gestión de turnos para barberías y estéticas

Sos un ingeniero de software senior. Vas a construir, de forma incremental, una plataforma SaaS de gestión de turnos para barberías, centros de estética y negocios similares. Esta especificación sale del documento funcional del proyecto. Leela completa antes de escribir código.

## Cómo quiero que trabajes

1. **Antes de codear, proponé un plan.** Resumí lo que entendiste, listá supuestos y dudas, y dividí el trabajo en hitos pequeños. Esperá mi confirmación del plan antes de empezar el Hito 1.
2. **Trabajá por hitos.** Cada hito debe compilar, tener pruebas pasando y poder demostrarse. No avances al siguiente sin cerrar el actual.
3. **Alcance de esta etapa: solo Fase 1 (MVP).** No implementes pagos, WhatsApp, campañas, comisiones ni inventario. Dejá preparados los puntos de extensión descritos más abajo, pero sin construir esas funciones.
4. **Pruebas primero en lo crítico.** La disponibilidad de horarios, la prevención de doble reserva y los permisos requieren pruebas de integración con PostgreSQL real (Testcontainers).
5. **Si algo es ambiguo, elegí la opción más simple que cumpla el requerimiento, dejá un comentario `// DECISIÓN:` y avisame en el resumen del hito.** No inventes funcionalidades que no estén acá.
6. **Commits chicos y descriptivos** (Conventional Commits). Un hito puede tener varios commits.
7. **Al cerrar cada hito**, resumí en pocas líneas qué quedó hecho, cómo probarlo y qué decisiones tomaste.
8. Creá un archivo `CLAUDE.md` en la raíz con los comandos de build, test y ejecución, las convenciones del proyecto y las decisiones de arquitectura. Mantenelo actualizado.

## Producto

Plataforma web (mobile first) donde cada negocio:

- Tiene una **página pública de reservas** en `/nombre-del-local` (el link se comparte en Instagram, Google Maps y WhatsApp).
- Puede ser **encontrado por nombre** desde un buscador en la página principal.
- Puede tener **varias sucursales** controladas desde una sola cuenta.
- Trabaja con **barberos (profesionales)** que eligen los servicios que realizan y fijan su propio precio.
- Gestiona permisos mediante **roles jerárquicos**.
- Avisa de los turnos **por email** (solo email; otros canales se evaluarán más adelante).
- Permite **login con Google** además de email.

Propuesta de valor: reserva sin fricción para el cliente, control fino de permisos para el negocio, precios y servicios por profesional, y sin comisiones por cliente nuevo.

## Stack (fijo, no lo cambies sin consultarme)

**Backend**
- Java 25 (LTS) con Spring Boot 4.1. Si alguna librería no es compatible todavía, usá la última 4.0.x y avisame cuál fue el motivo. Java 21 es el mínimo aceptable.
- Maven con wrapper.
- Spring Web MVC (REST) con hilos virtuales activados, Jakarta Bean Validation, MapStruct, springdoc-openapi (versión compatible con Boot 4).
- Spring Modulith para la estructura modular y la verificación de límites entre módulos.
- Spring Data JPA (Hibernate) para el CRUD. jOOQ o consultas nativas para el cálculo de disponibilidad.
- PostgreSQL 17 o superior. Flyway para migraciones. HikariCP.
- Spring Security: OAuth2 Client (Google, OpenID Connect), login con email y contraseña (Argon2 o BCrypt con el codificador delegante), sesión en servidor con cookie HttpOnly y SameSite, CSRF activo.
- Spring Mail + plantillas Thymeleaf. JobRunr para tareas programadas.
- Caffeine para caché local. Bucket4j para limitar la tasa de solicitudes.
- Errores con Problem Details (RFC 9457) y un manejador global.

**Frontend** (proyecto separado en el mismo repositorio)
- Next.js con TypeScript y React, con renderizado en servidor para las páginas públicas.
- Tailwind CSS y shadcn/ui. TanStack Query, React Hook Form y Zod.
- Agenda con FullCalendar (vista por profesional y por día) o una grilla propia.
- Tipos del cliente generados desde el OpenAPI del backend (openapi-typescript).
- PWA (manifest y service worker) como mejora posterior dentro de la Fase 1, no bloqueante.

**Calidad y entorno**
- JUnit 5, Mockito, AssertJ, Testcontainers (PostgreSQL), ArchUnit y las verificaciones de Spring Modulith. Playwright para el flujo de reserva completo.
- Spotless para formato.
- Docker Compose para desarrollo local: PostgreSQL y Mailpit (servidor de email de prueba).
- GitHub Actions: compilar, probar y analizar en cada push.
- Actuator + Micrometer, logs estructurados en JSON.

**IDE y entorno de desarrollo**
- Uso **IntelliJ IDEA** en **Windows 11**. El repositorio tiene que abrirse en IntelliJ abriendo la carpeta raíz y funcionar sin configuración manual.
- El backend se importa como proyecto Maven desde `backend/pom.xml`. Los procesadores de anotaciones (MapStruct, Hibernate, Spring Boot configuration processor) van en `annotationProcessorPaths` del `maven-compiler-plugin`, así IntelliJ los toma al importar.
- Configuraciones de ejecución compartidas en `.run/` (versionadas): levantar el backend con el perfil `dev`, correr todas las pruebas y levantar el frontend. Usá tipos de configuración que funcionen también en IntelliJ Community (Application, Maven, npm), no solo los de Ultimate.
- `.gitignore` excluye `.idea/` y los archivos `*.iml`, pero no `.run/`.
- `.editorconfig` en la raíz, coherente con Spotless. Elegí un formato de Spotless que tenga plugin para IntelliJ (palantir-java-format o google-java-format) y explicá en `CLAUDE.md` cómo configurarlo.
- JDK 25 documentado en el `README.md` como SDK del proyecto. En Windows los comandos usan `mvnw.cmd`; documentá ambas variantes (`./mvnw` y `mvnw.cmd`).
- Testcontainers y Docker Compose requieren Docker Desktop corriendo; indicalo en el `README.md`.

**Estructura del repositorio**

```
/
├── backend/        # Spring Boot (monolito modular)
├── frontend/       # Next.js
├── docker-compose.yml
├── CLAUDE.md
└── README.md
```

## Arquitectura

Monolito modular. Módulos del backend (paquete base `com.lacivita.turnos`):

| Módulo | Responsabilidad |
|---|---|
| `business` | Negocio, sucursales, link personalizado (slug) y buscador |
| `users` | Cuentas, login, roles y membresías |
| `catalog` | Servicios, barberos y precios |
| `schedule` | Disponibilidad, horarios, descansos y bloqueos |
| `booking` | Turnos, estados y políticas de cancelación |
| `notifications` | Canales, plantillas y envíos |

Reglas:
- Los módulos se comunican por interfaces públicas y por eventos de Spring/Modulith, no accediendo a las tablas ni a las entidades de otros módulos.
- La verificación de límites entre módulos debe estar en las pruebas.
- Los permisos viven en un solo lugar (ver Roles).

### Glosario (código en inglés, interfaz en español rioplatense)

| Término del documento | Nombre en código |
|---|---|
| Negocio | `Business` |
| Sucursal | `Branch` |
| Barbero / profesional | `Barber` |
| Servicio | `Service` |
| Servicio por barbero | `BarberService` |
| Turno | `Appointment` |
| Cliente | `Customer` |
| Membresía | `Membership` |
| Disponibilidad | `Availability` |
| Bloqueo | `TimeBlock` |

Roles: `ADMIN`, `OWNER`, `MANAGER`, `BARBER`, `CUSTOMER`.

## Roles y permisos

Jerarquía (de mayor a menor): `ADMIN > OWNER > MANAGER > BARBER > CUSTOMER`. Cada rol puede hacer todo lo que hacen los inferiores. Implementala con `RoleHierarchy` de Spring Security y `@PreAuthorize`, más un `PermissionEvaluator` central que valide el alcance por negocio y sucursal.

| Acción | ADMIN | OWNER | MANAGER | BARBER | CUSTOMER |
|---|---|---|---|---|---|
| Administrar la plataforma y soporte | Sí | No | No | No | No |
| Editar datos del negocio (nombre, link, logo, políticas) | Sí | Sí | No | No | No |
| Crear y administrar sucursales | Sí | Sí | No | No | No |
| Nombrar o quitar gerentes | Sí | Sí | No | No | No |
| Invitar o dar de baja barberos | Sí | Sí | Sí | No | No |
| Agregar o editar servicios del catálogo | Sí | Sí | Sí | No | No |
| Cambiar precios del catálogo | Sí | Sí | Sí | No | No |
| Definir precio propio y servicios que realiza | Sí | Sí | Sí | Propio | No |
| Gestionar horarios, descansos y bloqueos | Sí | Sí | Sí | Propio | No |
| Ver todas las reservas | Sí (soporte) | Sí | Su alcance | Su alcance | Propias |
| Crear, editar y cancelar turnos | Sí | Sí | Sí | Sí | Propios |
| Reservar un turno | Sí | Sí | Sí | Sí | Sí |
| Ver datos de contacto de clientes | Sí (soporte) | Sí | Sí | Clientes con turno | Propios |
| Ver reportes de ingresos y ocupación | Sí | Sí | Su alcance | Propios | No |
| Configurar notificaciones y plantillas | Sí | Sí | Sí | No | Preferencias propias |

"Propio" = solo sus datos. "Su alcance" = el negocio o la sucursal asignada.

Reglas de control:
- **Alcance por sucursal:** un gerente o barbero opera solo dentro de las sucursales que tiene asignadas. El dueño ve todas.
- **Delegación acotada:** solo el dueño nombra o quita gerentes. El gerente puede invitar barberos (reciben el rol `BARBER` al aceptar la invitación) y darlos de baja, pero no puede crear gerentes ni modificar al dueño.
- **Límite de precios configurable:** el dueño puede definir un rango mínimo y máximo dentro del cual cada barbero elige su precio. Fuera del rango, el cambio requiere aprobación del gerente.
- **Visibilidad de reservas configurable:** el barbero siempre ve su propia agenda. Por defecto también ve las reservas del resto de su sucursal; el dueño puede restringirlo a las propias y decidir si ve el teléfono de clientes ajenos.
- **Acceso de soporte auditado:** cuando un `ADMIN` entra a datos de un negocio, se registra quién, cuándo y por qué.
- **Registro de cambios:** toda modificación de precios, roles, turnos y servicios guarda usuario, fecha, valor anterior y valor nuevo (Spring Data Envers o tabla propia de auditoría).
- **El control de acceso se aplica siempre en el servidor**, nunca solo en la interfaz.

## Autenticación

- Botón "Continuar con Google" en registro e inicio de sesión (OpenID Connect). Se piden solo nombre, email y foto.
- Alternativa: email con contraseña, y link de acceso por email.
- **Vinculación de cuentas:** si ya existe una cuenta con el mismo email verificado, Google se vincula a esa cuenta en lugar de crear otra.
- Google solo identifica a la persona. Los roles dependen de la `Membership` en cada negocio. Un barbero invitado por email entra con Google y recibe su rol cuando el email coincide con el de la invitación.
- Google no siempre entrega el teléfono: se pide en la primera reserva y se guarda en el perfil.
- Reserva como invitado permitida, verificando el email con un código.
- Verificación anti bots (Cloudflare Turnstile o reCAPTCHA) en la reserva como invitado y límite de solicitudes en login y reservas (Bucket4j).
- Supuesto por defecto: el login con Google está disponible para todos los roles. Dejá la política configurable.

## Funcionalidades de la Fase 1

### Página pública y buscador
- Página pública en `/{slug}`: logo, fotos, descripción, sucursales con dirección y mapa, horarios, redes, equipo y catálogo. Sin competidores ni publicidad.
- Slug único en toda la plataforma: solo minúsculas, números y guiones; validación de disponibilidad en tiempo real; lista de palabras reservadas (`admin`, `login`, `buscar`, `api`, `soporte`, etc.) para que no pisen rutas de la app.
- Redirección automática cuando el negocio cambia su slug.
- Código QR descargable y botón para copiar el link.
- Metadatos para que Google y las vistas previas de WhatsApp e Instagram muestren bien la página (Open Graph y datos estructurados).
- Buscador en la home: por nombre, con autocompletado y tolerancia a errores de tipeo y tildes (extensiones `pg_trgm` y `unaccent`). Filtros por ciudad o barrio, tipo de negocio y servicio. Resultados con foto, nombre, zona y próximo turno disponible.
- El negocio decide si aparece en el buscador o solo se accede por link directo.

### Barberos, servicios y precios
1. El negocio define un **catálogo de servicios** (nombre, categoría, duración base, precio base de referencia).
2. Cada barbero **elige qué servicios realiza** del catálogo, o propone uno nuevo que el gerente aprueba.
3. Cada barbero puede fijar **precio y duración propios** por servicio. Si no lo hace, hereda los valores base.
4. Al reservar, el cliente ve el precio exacto del barbero elegido. Con "cualquiera disponible" se muestra "desde" el precio más bajo.
5. **El precio y la duración vigentes al reservar se guardan dentro del turno** (`AppointmentService` con snapshot), para que cambios posteriores no alteren turnos ni reportes históricos.
- Perfil público de cada barbero (foto, descripción, especialidades).
- Servicios combinados (por ejemplo corte y barba) que suman duración y precio.

### Sucursales
- Cada sucursal con dirección, mapa, teléfono, horarios, feriados y zona horaria propios.
- Un barbero puede trabajar en varias sucursales con horarios distintos, sin superposición entre ellas.
- El cliente elige sucursal antes que barbero. Si el negocio tiene una sola, ese paso se omite.
- Catálogo compartido entre sucursales y base de clientes compartida. Panel del dueño con vista por sucursal.

### Reservas (flujo del cliente, 3 pasos o menos)
- Elegir sucursal, servicio(s), profesional (o "cualquiera disponible") y horario.
- Disponibilidad en tiempo real.
- Confirmación inmediata por email con archivo `.ics` para agregar el turno al calendario.
- Cancelación y reprogramación desde un link seguro enviado por email, sin iniciar sesión, respetando la política de cancelación.
- Historial de turnos del cliente y "repetir última reserva".

### Panel del negocio
- Agenda en vista día y semana, por profesional y por sucursal, con colores según estado.
- Carga manual de turnos (clientes que llaman o llegan sin reserva).
- Bloqueo de horarios (trámites, almuerzo, capacitaciones). Gestión de horarios, descansos, vacaciones y feriados.
- Estados del turno: `PENDING`, `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `NO_SHOW`.
- Base de clientes con contacto, notas, preferencias e historial.
- Onboarding guiado: negocio, sucursal, servicios, equipo y horarios en menos de diez minutos.

## Lógica de negocio crítica

- **Cálculo de disponibilidad:** cruza el horario del profesional en la sucursal, la duración del servicio, los turnos existentes, los bloqueos y los descansos, más el tiempo de preparación configurable entre turnos.
- **Prevención de doble reserva en la base de datos**, no solo en el código:

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE appointment
  ADD CONSTRAINT no_overlapping_appointments
  EXCLUDE USING gist (
    barber_id WITH =,
    tstzrange(starts_at, ends_at, '[)') WITH &&
  )
  WHERE (status NOT IN ('CANCELLED', 'NO_SHOW'));
```

  Además, una **reserva temporal** del horario (unos 5 minutos) mientras el cliente completa el flujo. Tené una prueba que lance reservas simultáneas del mismo horario y verifique que solo una prospera.
- **Anticipación mínima y máxima** configurables (por ejemplo, no menos de 1 hora ni más de 60 días).
- **Asignación automática** con "cualquiera disponible": criterio configurable (menor carga, rotación o preferido del cliente).
- **Políticas de cancelación**: plazo límite configurable por negocio.
- **Fechas:** guardar en UTC (`timestamptz`) y convertir según la zona horaria de la sucursal con `java.time`. Considerar cambios de horario.
- **Multi-negocio (multi-tenancy):** una sola base con `business_id` en las tablas, filtro automático en la capa de datos y Row Level Security de PostgreSQL como segunda barrera. Un negocio nunca debe ver datos de otro (probalo).

## Notificaciones (Fase 1: solo email)

- Interfaz `NotificationChannel` con la implementación de email. La lógica de notificaciones debe ser independiente del canal, para poder sumar otros más adelante sin reescribirla.
- Eventos y destinatarios:

| Evento | Cliente | Barbero | Gerente o dueño |
|---|---|---|---|
| Turno creado | Email | Aviso en la app y email | Resumen opcional |
| Recordatorio (24 h y 2 h antes, configurable) | Email | Resumen diario de agenda | No |
| Turno modificado o reprogramado | Email | Aviso en la app y email | No |
| Turno cancelado | Email | Aviso en la app y email | Opcional |

- Patrón **outbox**: el aviso pendiente se guarda en la misma transacción que el turno y se envía después, con reintentos (JobRunr). No se deben perder notificaciones si falla el envío.
- Los recordatorios se programan con JobRunr y se reprograman o cancelan si el turno cambia.
- Plantillas HTML con Thymeleaf, editables por el negocio (variables: nombre, servicio, barbero, sucursal, dirección, hora). Botones de confirmar, reprogramar y cancelar mediante links seguros.
- Registro de envíos con estado (enviado, entregado, fallido) visible en la ficha del turno.
- En desarrollo, los emails salen a Mailpit. En producción, proveedor transaccional (Resend o Amazon SES) con SPF y DKIM.

## Modelo de datos inicial

| Entidad | Campos principales |
|---|---|
| Business | id, name, slug (único), logo, description, searchable, plan |
| Branch | id, business, name, address, coordinates, phone, timezone, opening hours |
| User | id, name, email, phone, password (opcional), linked providers (Google), email verified, global role |
| Membership | user, business, branch (opcional), role (OWNER, MANAGER, BARBER) |
| Service | id, business, name, category, base duration, base price, active |
| BarberService | barber, service, own price, own duration, active |
| Availability | barber, branch, weekday, from, to, breaks |
| TimeBlock | barber o branch, start, end, reason |
| Customer (por negocio) | business, user o contacto, notes, preferences, consents |
| Appointment | id, business, branch, barber, customer, starts_at, ends_at, status, total price, source (web, mostrador), created by |
| AppointmentService | appointment, service, price y duration vigentes al reservar |
| Notification / Outbox | appointment, channel, type, send status, date |
| AuditLog | user, action, entity, old value, new value, date |

Todas las tablas de negocio llevan `business_id`. Migraciones con Flyway, un script versionado por cambio.

## Requisitos no funcionales

- **Seguridad:** hash de contraseñas, HTTPS, CSRF, cabeceras de seguridad, control de acceso por rol en el servidor, protección contra abuso en formularios públicos.
- **Privacidad:** consentimiento para guardar datos personales y para enviar mensajes (Ley 25.326 de Argentina).
- **Rendimiento:** página pública de reservas en menos de 2 segundos en redes móviles comunes.
- **Accesibilidad:** contraste adecuado, navegación por teclado y textos legibles.
- **Idioma:** interfaz en español rioplatense (es-AR), con textos centralizados para poder traducirlos después.
- **Mobile first** en todo el frontend.

## Plan de hitos sugerido (ajustalo en tu plan)

1. **Base del proyecto:** estructura del repositorio, Docker Compose (PostgreSQL y Mailpit), esqueleto Spring Boot con Modulith, Flyway, CI, `CLAUDE.md` y `README.md`.
2. **Usuarios y autenticación:** registro, login con email y con Google, sesiones, roles, jerarquía y `PermissionEvaluator`.
3. **Negocios y sucursales:** alta de negocio con slug, sucursales, membresías e invitaciones, auditoría básica.
4. **Catálogo:** servicios, barberos, servicios por barbero con precios propios y rangos.
5. **Agenda y disponibilidad:** horarios, descansos, bloqueos, feriados y cálculo de disponibilidad, con la restricción anti doble reserva y sus pruebas.
6. **Reservas:** flujo de reserva (incluido invitado), estados, cancelación y reprogramación por link seguro, carga manual.
7. **Notificaciones por email:** outbox, plantillas, recordatorios y registro de envíos.
8. **Frontend público:** página `/{slug}`, flujo de reserva y buscador.
9. **Frontend del panel:** agenda, gestión de catálogo, equipo, sucursales y clientes.
10. **Endurecimiento:** pruebas de extremo a extremo, pruebas de carga de reservas simultáneas, observabilidad, límites de tasa y revisión de seguridad y de aislamiento entre negocios.

## Definición de terminado (por hito)

- Compila y pasa todas las pruebas (`./mvnw verify` y las del frontend).
- Pruebas de integración con PostgreSQL real para la lógica crítica.
- Sin violaciones de límites entre módulos (Modulith y ArchUnit).
- Permisos verificados con pruebas por rol.
- Migraciones reproducibles desde cero.
- `CLAUDE.md` y `README.md` actualizados.

## Fuera de alcance por ahora (no implementar)

Pagos y señas (Mercado Pago, Stripe), WhatsApp, campañas y fidelización, comisiones, caja, inventario, reseñas, lista de espera, turnos recurrentes, planes de suscripción SaaS y dominio propio. Dejá solo los puntos de extensión indicados (canal de notificaciones, modelo de roles ampliable).

## Decisiones pendientes (usá estos supuestos y avisame)

1. El barbero siempre ve su propia agenda y, por defecto, también las reservas de su sucursal, pero solo los datos de contacto de sus propios clientes (configurable por el dueño).
2. Los precios de cada barbero se limitan por rango definido por el dueño; fuera del rango requieren aprobación del gerente.
3. El login con Google se ofrece a todos los roles.
4. Frontend en Next.js separado, servido bajo el mismo dominio que la API mediante proxy inverso.

## Primer paso

Respondé con tu plan: supuestos, dudas, hitos con criterios de aceptación y riesgos. Todavía no escribas código.
