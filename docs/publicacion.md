# Publicación

Laciturnos está publicado como proyecto de portfolio, gratis y en los subdominios de cada servicio:

| Parte | Servicio | Dirección |
|---|---|---|
| Frontend | Vercel | https://laciturnos.vercel.app |
| Backend (Docker) | Render, plan gratis | https://laciturnos-api.onrender.com |
| Base de datos | Neon (PostgreSQL 17) | Conexión directa, región AWS US East 1 |

El navegador habla solo con Vercel; Vercel pasa `/api/*` al backend, así la cookie de sesión queda en un solo dominio. Cada push a `main` vuelve a publicar los dos.

## Modo demo

Laciturnos se publica como proyecto de portfolio. En el modo demo:

- **Los emails no se envían**: quedan en el buzón de la demo (`/demo/emails`), con su HTML, sus links y el `.ics`. Donde la app dice "te mandamos un email" aparece un atajo al buzón. Cualquiera que escriba una dirección ve sus emails: la página pide usar direcciones inventadas. Se borran a los 3 días.
- **Datos de ejemplo**: "Barbería El Faro" (`/barberia-el-faro`) con dos sucursales, tres profesionales, servicios, un combo, clientes y turnos alrededor de hoy. Se reinicia en cada arranque del backend.
- **Cuentas de ejemplo** (contraseña `demo1234`, pública a propósito), con "Entrar" de un toque en `/demo`:

| Cuenta | Rol |
|---|---|
| `duena.demo@example.com` | Dueña |
| `gerente.demo@example.com` | Gerente de las dos sucursales |
| `nico.demo@example.com`, `juli.demo@example.com`, `sofi.demo@example.com` | Profesionales |
| `cliente.demo@example.com` | Cliente con turnos |

Se activa con el perfil `demo` del backend (se suma a otro: `prod,demo` o `dev,demo`) y `NEXT_PUBLIC_DEMO_MODE=true` en el frontend. Los datos de ejemplo están en `backend/src/main/resources/db/demo/R__demo_data.sql`: una migración repetible de Flyway que solo se suma con el perfil `demo`.

## Cómo publicarlo

Los pasos para publicarlo desde cero, en este orden:

1. **Neon** (base de datos). Crear un proyecto en la región *AWS US East 1 (N. Virginia)* con PostgreSQL 17. En el **SQL Editor**, crear el rol de la aplicación (sin privilegios, así le aplica Row Level Security) con una contraseña larga:

   ```sql
   CREATE ROLE turnos_app LOGIN PASSWORD '<contraseña>' NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS;
   ```

   Usar la conexión **directa** (el host *sin* `-pooler`): la aplicación fija el negocio de cada conexión y el pooler de Neon las comparte entre clientes. Las migraciones las corre el dueño de la base (`neondb_owner`).

   Si hay que cambiar la contraseña de `turnos_app`, lo más seguro es **Roles → `turnos_app` → ⋮ → Reset password** en Neon y copiar la que genera a `DATABASE_PASSWORD` de Render (después, **Save, rebuild, and deploy**). Escribirla a mano en dos lugares es la causa más común de `password authentication failed`.

2. **Cloudflare Turnstile** (anti-bots de la reserva como invitado). En el panel de Cloudflare, **Turnstile → Add widget**, con el dominio `<proyecto>.vercel.app`. Da una clave pública (frontend) y una secreta (backend).

3. **Render** (backend). **New → Blueprint** con este repositorio: toma `render.yaml` (Docker, plan gratis, perfiles `prod,demo`). Completar las variables:

| Variable | Valor |
|---|---|
| `DATABASE_URL` | `jdbc:postgresql://<host directo de Neon>/neondb?sslmode=require` |
| `DATABASE_PASSWORD` | La contraseña de `turnos_app` |
| `FLYWAY_USERNAME`, `FLYWAY_PASSWORD` | `neondb_owner` y su contraseña (Neon → Connection details) |
| `FRONTEND_URL` | `https://<proyecto>.vercel.app` |
| `TURNSTILE_SECRET_KEY` | La clave secreta de Turnstile |

   En el plan gratis el servicio se duerme a los 15 minutos sin uso: la primera visita después tarda alrededor de un minuto. Al despertar, la demo vuelve a sus datos de ejemplo.

4. **Vercel** (frontend). **Add New → Project** con este repositorio, **Root Directory** `frontend`. Variables de entorno:

| Variable | Valor |
|---|---|
| `BACKEND_URL` | `https://laciturnos-api.onrender.com` (la URL que da Render) |
| `NEXT_PUBLIC_SITE_URL` | `https://<proyecto>.vercel.app` |
| `NEXT_PUBLIC_DEMO_MODE` | `true` |
| `NEXT_PUBLIC_TURNSTILE_SITE_KEY` | La clave pública de Turnstile |

Cada push a `main` vuelve a publicar los dos. Para un uso real (fuera de la demo): sacar `demo` de `SPRING_PROFILES_ACTIVE` y `NEXT_PUBLIC_DEMO_MODE`, y configurar Resend (ver "Emails reales" más abajo).

## Anti-bots (Cloudflare Turnstile)

La reserva como invitado pide la verificación de Turnstile antes de mandar el código. En desarrollo y en las pruebas está desactivada (la aplicación lo avisa en el log). En producción es obligatoria: sin `TURNSTILE_SECRET_KEY` el backend no arranca.

## Emails reales (Resend)

Fuera de la demo, los emails salen por [Resend](https://resend.com), por SMTP. Para configurarlo:

1. Crear una cuenta en Resend.
2. En **Domains**, agregar el dominio desde el que salen los emails (por ejemplo `laciturnos.com.ar`) y cargar en el DNS del dominio los registros que indica Resend (SPF y DKIM). Esperar a que figure como verificado.
3. En **API Keys**, crear una clave con permiso **Sending access**, limitada a ese dominio.
4. Configurar las variables de entorno del servidor:

| Variable | Valor |
|---|---|
| `MAIL_PASSWORD` | La API key de Resend (empieza con `re_`). Es secreta: nunca va en el repositorio |
| `MAIL_FROM` | Una dirección del dominio verificado, por ejemplo `turnos@laciturnos.com.ar` |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME` | Opcionales: por defecto `smtp.resend.com`, `587` y `resend` |

La conexión exige TLS. El registro de envíos marca `SENT` cuando Resend acepta el email; el estado "entregado" requiere sumar los webhooks de Resend.

Las tablas de JobRunr las crea Flyway (`V14`). Al actualizar JobRunr, revisar si trae migraciones nuevas y agregarlas en una migración propia.

