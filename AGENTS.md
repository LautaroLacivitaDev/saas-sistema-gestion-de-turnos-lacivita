# AGENTS.md

Instrucciones para cualquier agente de IA (Claude Code, Codex, Cursor, etc.) que trabaje en este repositorio. Leé este archivo y [MEMORY.md](MEMORY.md) antes de empezar cualquier tarea.

## Proyecto

Plataforma SaaS de gestión de turnos para barberías, centros de estética y negocios similares. Cada negocio tiene su página pública de reservas en `/{slug}`, puede tener varias sucursales y trabaja con barberos que fijan sus propios servicios y precios.

- Especificación del MVP (fuente de verdad): [docs/especificacion-mvp.md](docs/especificacion-mvp.md)
- Estado actual, decisiones tomadas y pendientes: [MEMORY.md](MEMORY.md)
- Alcance actual: **solo Fase 1 (MVP)**. No implementar pagos, WhatsApp, campañas, comisiones, caja, inventario, reseñas, lista de espera, turnos recurrentes, planes SaaS, dominio propio ni rol de recepción.

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

Direcciones en desarrollo: API en http://localhost:8080/api, Swagger UI en http://localhost:8080/api/docs, frontend en http://localhost:3000, Mailpit en http://localhost:8025.

Antes de dar por terminado un cambio: `verify` en el backend y lint, typecheck, test y build en el frontend tienen que pasar.

**Migraciones:** nunca modificar un archivo de `db/migration` ya commiteado, ni siquiera un comentario. Flyway valida el checksum y el arranque falla. Cualquier cambio va en una migración nueva.

**Frontend:** Next.js 16 tiene cambios incompatibles con versiones anteriores. Antes de escribir código de Next, leé [frontend/AGENTS.md](frontend/AGENTS.md) y la guía correspondiente en `frontend/node_modules/next/dist/docs/`.

## Entorno

- Windows 11, IntelliJ IDEA Community para el backend y VS Code para el frontend.
- JDK 25 (Eclipse Temurin), Node 24, Docker Desktop (con WSL 2).
- Testcontainers y Docker Compose requieren Docker Desktop corriendo.
- Configuraciones de ejecución compartidas en `.run/` (versionadas). `.idea/` y `*.iml` no se versionan.
- Usar tipos de configuración que funcionen en IntelliJ Community (Application, Maven, npm).

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
- Toda tabla de negocio lleva `business_id`. Filtro automático en la capa de datos más Row Level Security en PostgreSQL.
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
- **Ambigüedades:** elegir la opción más simple que cumpla el requerimiento, dejar un comentario `// DECISIÓN:` y registrarla en [MEMORY.md](MEMORY.md).
- No inventar funcionalidades que no estén en la especificación.

## Pruebas

- La disponibilidad, la prevención de doble reserva, los permisos y el aislamiento entre negocios se prueban con PostgreSQL real (Testcontainers).
- Cada regla de permisos tiene pruebas por rol.
- Las migraciones tienen que correr desde una base vacía.

## Forma de trabajo

1. Trabajar por hitos (lista en [MEMORY.md](MEMORY.md)). No avanzar al siguiente sin cerrar el actual.
2. Un hito está terminado cuando: compila, pasan todas las pruebas (backend y frontend), no hay violaciones de límites entre módulos, los permisos están probados por rol, las migraciones son reproducibles y la documentación está actualizada.
3. Al cerrar un hito: resumir qué quedó hecho, cómo probarlo y qué decisiones se tomaron, y actualizar [MEMORY.md](MEMORY.md) y la tabla de comandos de este archivo.
4. No cambiar el stack sin consultar.
