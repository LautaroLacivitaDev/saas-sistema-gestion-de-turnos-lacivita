# Guía de desarrollo

Cómo levantar Laciturnos en tu PC, correr las pruebas y trabajar con el IDE.

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

PostgreSQL usa el puerto **5433** para no chocar con una instalación local en el 5432. El usuario `turnos_app` lo crea `docker/postgres/init/01-app-role.sql` la primera vez que se crea el volumen. Si tu base es anterior a ese script, crealo una vez a mano:

```bash
docker compose exec postgres psql -U turnos -d turnos -f /docker-entrypoint-initdb.d/01-app-role.sql
```

### 2. Backend

```bash
cd backend
mvnw.cmd spring-boot:run     # Windows
./mvnw spring-boot:run       # macOS / Linux
```

- API: http://localhost:8080/api
- Documentación de la API (Swagger UI): http://localhost:8080/api/docs
- Estado: http://localhost:8080/actuator/health

Sin perfil explícito arranca con `dev`. Las migraciones de Flyway se aplican solas al iniciar. Para usar el **modo demo** (buzón de emails y "Barbería El Faro" con datos de ejemplo):

```bash
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev,demo     # Windows
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,demo       # macOS / Linux
```

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Abrí http://localhost:3000. Las llamadas a `/api/*` se pasan al backend en `localhost:8080`. Para cambiar esa dirección, activar el modo demo (`NEXT_PUBLIC_DEMO_MODE=true`) o la clave de Turnstile, copiá `.env.example` como `.env.local`.

Los tipos de la API (`src/lib/api/schema.d.ts`) se generan desde el OpenAPI del backend. Con el backend corriendo, después de cambiar un endpoint:

```bash
npm run api:types
```

En desarrollo los emails llegan a Mailpit (http://localhost:8025); con el perfil `demo`, al buzón de la demo (`/demo/emails`).

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

Si `verify` falla por formato, corré `mvnw.cmd spotless:apply` (o `./mvnw spotless:apply`) y volvé a probar. Las pruebas del backend corren con la zona horaria de Buenos Aires, así un error que dependa de la zona del servidor falla también en CI (que corre en UTC).

### De punta a punta (Playwright)

Corren en un celular (Pixel 7) y en escritorio contra la app en modo demo: reserva como invitado con el código del buzón, elegir el servicio antes que la sucursal, reservar y cancelar desde "Mis turnos", entrada al panel y ausencia de scroll horizontal a 320 px. Con el backend (`dev,demo`) y el frontend (`NEXT_PUBLIC_DEMO_MODE=true`) corriendo:

```bash
cd frontend
npx playwright install chromium   # la primera vez
npm run e2e
```

En CI corren en el job "Punta a punta".

### De carga (k6)

`load/same-slot.js` manda a 100 personas a tomar el mismo horario a la vez: tiene que ganar una sola y el resto recibir 409, sin errores. Como todos los pedidos salen de la misma IP, el backend se levanta con un límite de intentos alto:

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,demo "-Dspring-boot.run.arguments=--app.rate-limit.requests-per-minute=100000"
# en otra terminal, desde la raíz
docker run --rm -i --add-host=host.docker.internal:host-gateway grafana/k6:1.3.0 run -e BASE_URL=http://host.docker.internal:8080 - < load/same-slot.js
```

## Login con Google

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

## IDE

### IntelliJ IDEA (backend)

1. **File → Open** y elegí la carpeta raíz del repositorio. IntelliJ importa el backend como proyecto Maven automáticamente (para eso está el `pom.xml` de la raíz).
2. En **File → Project Structure → SDK** elegí el JDK 25.
3. Las configuraciones de ejecución ya vienen incluidas (carpeta `.run/`): *Backend (dev)*, *Backend - verify*, *Backend - formatear*, *Docker Compose - levantar* y *Frontend (dev)*.
4. Para que el formato de IntelliJ coincida con Spotless, instalá el plugin **palantir-java-format** desde el Marketplace y activalo en **Settings → palantir-java-format**.

Funciona con IntelliJ IDEA Community.

### VS Code (frontend)

Abrí la carpeta `frontend/`. VS Code te va a sugerir las extensiones recomendadas (ESLint, Tailwind CSS, Vitest y Claude Code).

## Estructura del repositorio

```
/
├── backend/                 # Spring Boot 4.1, monolito modular (Spring Modulith)
│   ├── src/main/java/com/lacivita/turnos/
│   │   ├── business/        # Negocios, sucursales, link público y buscador
│   │   ├── users/           # Cuentas, login, equipo e invitaciones
│   │   ├── catalog/         # Servicios, combos y precios de cada profesional
│   │   ├── schedule/        # Horarios, feriados, bloqueos y disponibilidad
│   │   ├── booking/         # Turnos, reservas, clientes y políticas de cancelación
│   │   ├── notifications/   # Emails, recordatorios, avisos en la app y buzón de la demo
│   │   └── shared/          # Seguridad, aislamiento entre negocios, auditoría y errores
│   ├── src/main/resources/db/migration/   # Migraciones de Flyway
│   ├── src/main/resources/db/demo/        # Datos de ejemplo (solo perfil demo)
│   └── Dockerfile
├── frontend/                # Next.js 16, TypeScript, Tailwind CSS, shadcn/ui
│   ├── src/app/(sitio)/     # Sitio público: buscador, página del negocio, reserva, cuenta, demo
│   ├── src/app/panel/       # Panel del negocio
│   ├── src/components/      # Componentes por área (booking, panel, account, demo, ui)
│   ├── src/messages/es-AR.ts # Todos los textos de la interfaz
│   └── e2e/                 # Pruebas de Playwright
├── load/                    # Prueba de carga con k6
├── docs/                    # Especificación, plan, decisiones y guías
├── docker/                  # Inicialización de PostgreSQL local
├── .github/workflows/       # CI: build y pruebas en cada push
├── .run/                    # Configuraciones de ejecución de IntelliJ
├── docker-compose.yml       # PostgreSQL y Mailpit para desarrollo
└── render.yaml              # Despliegue del backend en Render
```
