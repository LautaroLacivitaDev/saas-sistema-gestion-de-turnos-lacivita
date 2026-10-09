# AGENTS.md

Instrucciones para cualquier agente de IA (Claude Code, Codex, Cursor, etc.) que trabaje en este repositorio. Leé este archivo y [MEMORY.md](MEMORY.md) antes de empezar cualquier tarea.

## Proyecto

**Laciturnos**: plataforma SaaS de gestión de turnos para barberías, centros de estética y negocios similares. Cada negocio tiene su página pública de reservas en `/{slug}`, puede tener varias sucursales y trabaja con barberos que fijan sus propios servicios y precios.

- Especificación del MVP (fuente de verdad): [docs/especificacion-mvp.md](docs/especificacion-mvp.md)
- Plan de hitos y su estado: [docs/plan-mvp.md](docs/plan-mvp.md)
- Registro completo de decisiones: [docs/decisiones.md](docs/decisiones.md)
- Estado actual y aprendizajes: [MEMORY.md](MEMORY.md)
- Uso principal: **desde el celular**. Todo es mobile first (ver "Mobile first" más abajo).
- Alcance actual: **solo Fase 1 (MVP)**. No implementar pagos, WhatsApp, campañas, comisiones, caja, inventario, reseñas, lista de espera, turnos recurrentes, planes SaaS, dominio propio ni rol de recepción.

## Forma de trabajo esperada

Trabajá como un **desarrollador senior** en un proyecto que tiene que **escalar** y mantenerse durante años: código **modular**, con **encapsulamiento** estricto y buenas prácticas de **programación orientada a objetos y de Java**. Cada decisión de diseño tiene que poder justificarse. Si una solución rápida contradice estas reglas, elegí la correcta o consultá antes.

### Diseño orientado a objetos

- **Encapsulamiento:** atributos `private`, sin setters públicos en el dominio. El estado cambia solo mediante métodos con intención de negocio que validan sus reglas (`appointment.cancel(policy, now)`, no `setStatus(CANCELLED)`). Un objeto nunca queda en un estado inválido.
- **Dominio rico:** las reglas de negocio viven en las entidades y objetos de valor. Los servicios de aplicación orquestan (transacción, permisos, repositorios, eventos), no deciden reglas.
- **Objetos de valor inmutables** con `record` para conceptos con reglas propias (`Slug`, `Email`, `Money`, `TimeRange`, `PhoneNumber`). Se validan en el constructor: si existe, es válido.
- **SOLID**, composición antes que herencia, e interfaces solo donde hay variación real o un punto de extensión (por ejemplo `NotificationChannel`).
- **Colecciones:** nunca exponer una colección interna modificable. Devolver copias o vistas inmodificables.
- **Igualdad:** entidades por identidad, objetos de valor por valor.

### Encapsulamiento entre módulos

- Cada módulo expone una **API pública mínima** en su paquete raíz (interfaces de servicio, DTOs como `record` y eventos). Todo lo demás va en subpaquetes internos, que Spring Modulith trata como privados.
- Visibilidad mínima: clases e interfaces **package-private por defecto**. `public` solo cuando otro paquete realmente lo necesita.
- Estructura sugerida dentro de cada módulo:

  ```
  booking/
  ├── BookingApi.java, AppointmentBooked.java, ...   # API pública y eventos
  ├── domain/          # Entidades, objetos de valor, reglas, repositorios (Spring Data) y puertos
  ├── application/     # Casos de uso (orquestación, transacciones, permisos)
  ├── infrastructure/  # Implementaciones de puertos del dominio (cifrado, consultas nativas, clientes externos)
  └── web/             # Controladores REST, DTOs de entrada y salida, endpoints públicos del módulo
  ```

- **Dependencias entre capas** (las verifica `ArchitectureTests`): `web → application → domain ← infrastructure`. El dominio no depende de ninguna otra capa ni de HTTP; nadie referencia la infraestructura directamente, se inyecta por la interfaz del dominio; la web nunca expone entidades.
- **Endpoints públicos:** un módulo que tenga endpoints sin sesión los declara con un bean `PublicEndpoints` (y marca cuáles llevan límite de intentos). La seguridad común no conoce las rutas de los módulos.

- Nunca exponer entidades JPA fuera del módulo ni en la API REST: se mapean a DTOs (MapStruct).
- Entre módulos: llamadas a la API pública para consultas y **eventos** para efectos secundarios (avisar, auditar), así un módulo no depende de quién reacciona.

### Java

- Java 25 moderno: `record`, `sealed` para jerarquías cerradas (estados, resultados), `switch` con pattern matching, `var` solo cuando el tipo es obvio, text blocks para SQL.
- **Inyección por constructor**, dependencias `final`. Nada de `@Autowired` en campos (lo verifica ArchUnit).
- **Nulos:** `Optional` solo como tipo de retorno; nunca devolver `null` en colecciones; validar en los bordes (Bean Validation en la API, constructores en el dominio).
- **Excepciones:** de dominio específicas y con nombre de negocio (`SlotNoLongerAvailableException`), traducidas a Problem Details en el manejador global. Nunca capturar y silenciar.
- **Dinero** con `BigDecimal` (o un `Money` propio), nunca `double`. **Fechas** con `java.time` (`Instant` para persistir, `ZonedDateTime` para mostrar) y un `Clock` inyectado para poder probar.
- **Transacciones** en la capa de aplicación (`@Transactional`), con `readOnly = true` en las lecturas.
- Métodos cortos con un solo propósito, nombres que expresan intención, sin código muerto. Los comentarios explican el *por qué*, no el *qué*.

### Escalabilidad

- Servidores sin estado propio en memoria: todo lo que deba compartirse entre instancias va a la base de datos (o a Redis si se justifica).
- Listados siempre paginados. Evitar N+1 con consultas específicas o *fetch joins*. Cada consulta frecuente tiene su índice, creado en una migración.
- Caché solo para datos que cambian poco y con una regla clara de invalidación.
- Lo que no necesita respuesta inmediata (emails, recordatorios) se procesa de forma asíncrona con reintentos.

### Mobile first (la app se usa sobre todo desde el celular)

Clientes, barberos y dueños entran mayormente desde el teléfono. Todo lo que se haga tiene que funcionar primero en una pantalla chica y después escalar a tablet y escritorio.

- **Diseñar desde 360 px:** los estilos base son los del celular y los prefijos `sm:`, `md:`, `lg:` agregan lo de pantallas grandes, nunca al revés. Probar siempre a **320, 375 y escritorio**: sin scroll horizontal ni texto cortado.
- **Táctil:** zonas tocables de al menos 44 × 44 px con espacio entre sí. Nada que dependa del *hover*. La acción principal de cada pantalla, abajo y al alcance del pulgar (por ejemplo, "Reservar" fija al pie).
- **Formularios:** inputs con letra de 16 px o más (si no, iOS hace zoom), el `type`, `inputMode` y `autoComplete` que correspondan (teléfono con teclado numérico, email, nombre) y los mínimos campos posibles.
- **Pantalla:** usar `dvh` (no `vh`) para alturas completas y respetar las zonas seguras del notch con `env(safe-area-inset-*)` (ya configurado en el layout). Nunca bloquear el zoom.
- **Contenido:** una columna en el celular. Las tablas del panel se muestran como listas o tarjetas en pantalla chica. Agendas y calendarios: botón "Día / Semana" en cualquier pantalla; por defecto Día en el celular y Semana desde tablet.
- **Rendimiento en datos móviles:** imágenes con `next/image` y tamaños responsivos, poco JavaScript en el cliente (Server Components por defecto) y la página pública renderizada en el servidor.
- **API pensada para el celular:** respuestas chicas y paginadas, con lo justo para cada pantalla, para que cargue rápido con 4G.

### Pruebas como parte del diseño

- Las reglas de dominio se prueban con **pruebas unitarias sin Spring**: rápidas y sin base de datos.
- Persistencia, permisos, concurrencia y aislamiento entre negocios se prueban con **integración contra PostgreSQL real**.
- Nombres de prueba que describen el comportamiento (`cancelingAfterDeadlineIsRejected`) y estructura *given / when / then*.

## Estructura del repositorio

```
/
├── backend/          # Spring Boot, monolito modular (paquete base com.lacivita.turnos)
├── frontend/         # Next.js + TypeScript
├── docs/             # Especificación y documentación
├── docker-compose.yml
├── AGENTS.md         # Este archivo
├── CLAUDE.md         # Importa AGENTS.md y MEMORY.md para Claude Code
├── MEMORY.md         # Memoria del proyecto
└── README.md
```

## Comandos

Documentar siempre la variante de Windows (`mvnw.cmd`) y la de Unix (`./mvnw`). Los comandos del backend se corren desde `backend/` y los del frontend desde `frontend/`.

| Tarea | Windows | Unix |
|---|---|---|
| Levantar PostgreSQL (puerto 5433) y Mailpit | `docker compose up -d` | `docker compose up -d` |
| Compilar, chequear formato y correr todas las pruebas del backend | `mvnw.cmd verify` | `./mvnw verify` |
| Ejecutar el backend (perfil `dev` por defecto) | `mvnw.cmd spring-boot:run` | `./mvnw spring-boot:run` |
| Formatear el backend | `mvnw.cmd spotless:apply` | `./mvnw spotless:apply` |
| Frontend en desarrollo | `npm run dev` | `npm run dev` |
| Lint, tipos, pruebas y build del frontend | `npm run lint`, `npm run typecheck`, `npm test`, `npm run build` | igual |
| Regenerar los tipos de la API (con el backend corriendo) | `npm run api:types` | `npm run api:types` |

Direcciones en desarrollo: API en http://localhost:8080/api, Swagger UI en http://localhost:8080/api/docs, frontend en http://localhost:3000, Mailpit en http://localhost:8025.

Antes de dar por terminado un cambio: `verify` en el backend y lint, typecheck, test y build en el frontend tienen que pasar.

**Migraciones:** nunca modificar un archivo de `db/migration` ya commiteado, ni siquiera un comentario. Flyway valida el checksum y el arranque falla. Cualquier cambio va en una migración nueva.

**Frontend:** Next.js 16 tiene cambios incompatibles con versiones anteriores. Antes de escribir código de Next, leé [frontend/AGENTS.md](frontend/AGENTS.md) y la guía correspondiente en `frontend/node_modules/next/dist/docs/`.

## Entorno

- Windows 11, IntelliJ IDEA Community para el backend y VS Code para el frontend.
- JDK 25 (Eclipse Temurin), Node 24, Docker Desktop (con WSL 2).
- Testcontainers y Docker Compose requieren Docker Desktop corriendo.
- Configuraciones de ejecución compartidas en `.run/` (versionadas). `.idea/` y `*.iml` no se versionan.
- Usar tipos de configuración que funcionen en IntelliJ Community (Application, Maven, Shell Script). Community no tiene configuraciones de npm.

## Arquitectura

Monolito modular con Spring Modulith. Módulos bajo `com.lacivita.turnos`:

| Módulo | Responsabilidad |
|---|---|
| `business` | Negocio, sucursales, slug y buscador |
| `users` | Cuentas, login, roles y membresías |
| `catalog` | Servicios, barberos y precios |
| `schedule` | Disponibilidad, horarios, descansos y bloqueos |
| `booking` | Turnos, estados y políticas de cancelación |
| `notifications` | Canales, plantillas y envíos |
| `shared` | Seguridad, aislamiento entre negocios, auditoría y manejo de errores |

Reglas:

- Los módulos se comunican por su API pública y por eventos. Nunca acceder a entidades, repositorios ni tablas de otro módulo.
- La verificación de límites (Spring Modulith y ArchUnit) forma parte de las pruebas y no puede fallar.
- Los permisos viven en un solo lugar: el `PermissionEvaluator`. El control de acceso se aplica siempre en el servidor.
- Toda tabla de negocio lleva `business_id`. Filtro automático en la capa de datos más Row Level Security en PostgreSQL. La aplicación se conecta con `turnos_app` (sin privilegios para saltear RLS) y Flyway con el dueño de las tablas.
- El contexto de aislamiento (`TenantContext`: negocio, persona, sistema) se fija **antes** de abrir la transacción: `@BusinessScoped` + `@BusinessId` en los casos de uso. Cruzar negocios solo con `TenantContext.callAsSystem(motivo, …)`. El código que entrega conexiones nunca lee `SecurityContextHolder` (carga la sesión desde la base y pide otra conexión).
- Fechas en UTC (`timestamptz`), convertidas con `java.time` según la zona horaria de la sucursal.
- La prevención de doble reserva se hace en la base de datos (restricción de exclusión), no solo en el código.

## Convenciones

- **Idioma:** código, nombres de tablas y commits en inglés. Interfaz, mensajes al usuario y documentación en español rioplatense (es-AR). Textos del frontend centralizados.
- **Glosario:** Negocio `Business`, Sucursal `Branch`, Barbero `Barber`, Servicio `Service`, Servicio por barbero `BarberService`, Turno `Appointment`, Cliente `Customer`, Membresía `Membership`, Disponibilidad `Availability`, Bloqueo `TimeBlock`.
- **Roles:** `ADMIN`, `OWNER`, `MANAGER`, `BARBER`, `CUSTOMER`.
- **Commits:** chicos y con [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `test:`, `chore:`, `docs:`, `refactor:`).
- **Migraciones:** Flyway, un script versionado por cambio. Nunca editar una migración ya commiteada.
- **Errores de la API:** Problem Details (RFC 9457) desde un manejador global.
- **Formato:** Spotless. Correrlo antes de cada commit.
- **Ambigüedades:** elegir la opción más simple que cumpla el requerimiento, dejar un comentario `// DECISIÓN:` y registrarla en [docs/decisiones.md](docs/decisiones.md) (y en [MEMORY.md](MEMORY.md) si es importante).
- No inventar funcionalidades que no estén en la especificación.

## Pruebas

- La disponibilidad, la prevención de doble reserva, los permisos y el aislamiento entre negocios se prueban con PostgreSQL real (Testcontainers).
- Cada regla de permisos tiene pruebas por rol.
- En pruebas con MockMvc, el CSRF se manda con `SpaCsrf.spaCsrf()`, nunca con `csrf()` de Spring Security Test (modifica el filtro del contexto compartido).
- Las migraciones tienen que correr desde una base vacía.

## Forma de trabajo


1. Un hito está terminado cuando: compila, pasan todas las pruebas (backend y frontend), no hay violaciones de límites entre módulos, los permisos están probados por rol, las migraciones son reproducibles, toda pantalla nueva se probó en celular (320 y 375 px) y en escritorio, y la documentación está actualizada.
2. Al cerrar un hito: resumir qué quedó hecho, cómo probarlo y qué decisiones se tomaron, y actualizar [MEMORY.md](MEMORY.md), el estado en [docs/plan-mvp.md](docs/plan-mvp.md), [docs/decisiones.md](docs/decisiones.md) y la tabla de comandos de este archivo.
3. No cambiar el stack sin consultar.
4. Al terminar, resume qué has cambiado y cualquier decisión que deba revisar.

## Memoria
- Al empezar, lee `MEMORY.md` para conocer el estado del proyecto y las decisiones
  tomadas.
- Al terminar una tarea, actualízalo: estado actual, decisiones importantes (con su
  porqué) y errores a evitar.
- Mantenlo breve (máximo ~50 líneas): resume o elimina lo que ya no aporte.
- Si algo se convierte en una regla permanente, propón moverlo a `AGENTS.md` en lugar de
  dejarlo en la memoria.
- No guardes nunca datos sensibles (claves, tokens, datos personales).
