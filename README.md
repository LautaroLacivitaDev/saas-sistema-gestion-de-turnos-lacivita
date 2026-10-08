# Turnos: gestión de turnos para barberías y estéticas

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
| PostgreSQL 17 | `localhost:5433`, base `turnos`, usuario y contraseña `turnos` |
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
