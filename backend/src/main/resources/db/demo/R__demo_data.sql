-- Datos de ejemplo del modo demo (perfil "demo"). Nunca corre fuera de la demo: esta carpeta solo se suma a
-- las ubicaciones de Flyway en application-demo.yml.
--
-- Se vuelve a aplicar en cada arranque (por el marcador de abajo, que cambia siempre) y deja el negocio de
-- ejemplo como nuevo, con los turnos alrededor del día de hoy. Lo que crean los visitantes en otros
-- negocios no se toca.
--
-- Arranque: ${flyway:timestamp}
--
-- Cuentas (contraseña demo1234, pública a propósito: se muestra en la página para entrar a probar):
--   duena.demo@example.com     dueña de "Barbería El Faro"
--   gerente.demo@example.com   gerente de las dos sucursales
--   nico.demo@example.com      profesional en Palermo
--   juli.demo@example.com      profesional en Palermo
--   sofi.demo@example.com      profesional en Belgrano
--   cliente.demo@example.com   cliente con turnos

-- Fecha y hora de Buenos Aires a partir de hoy; si cae domingo (cerrado), pasa al lunes.
CREATE FUNCTION pg_temp.demo_at(day_offset INTEGER, local_time TIME) RETURNS TIMESTAMPTZ
    LANGUAGE plpgsql AS
$$
DECLARE
    day DATE := (now() AT TIME ZONE 'America/Argentina/Buenos_Aires')::DATE + day_offset;
BEGIN
    IF EXTRACT(ISODOW FROM day) = 7 THEN
        day := day + 1;
    END IF;
    RETURN (day + local_time) AT TIME ZONE 'America/Argentina/Buenos_Aires';
END
$$;

-- Se borra el negocio de ejemplo. Primero lo que apunta a servicios y sucursales sin cascada (turnos, combos
-- y servicios de cada profesional); el resto cae en cascada desde el negocio.
DELETE FROM appointment WHERE business_id = 'd0000000-0000-7000-8000-000000000001';
DELETE FROM combo WHERE business_id = 'd0000000-0000-7000-8000-000000000001';
DELETE FROM barber_service WHERE business_id = 'd0000000-0000-7000-8000-000000000001';
DELETE FROM business WHERE id = 'd0000000-0000-7000-8000-000000000001';

-- Cuentas: se actualizan en lugar de borrarse (pueden tener datos en negocios creados por visitantes).
INSERT INTO user_account (id, name, email, phone, password_hash, email_verified_at, platform_role, created_at, updated_at)
VALUES ('d0000000-0000-7000-8000-000000000101', 'Lucía Faro', 'duena.demo@example.com', NULL,
        '{bcrypt}$2a$10$U3eVOWb9sQLYAn/MTySdPO1X/a.5XJFuORdyrb7ved5/Qb.gscbS.', now(), 'USER', now(), now()),
       ('d0000000-0000-7000-8000-000000000102', 'Marcos Paz', 'gerente.demo@example.com', NULL,
        '{bcrypt}$2a$10$U3eVOWb9sQLYAn/MTySdPO1X/a.5XJFuORdyrb7ved5/Qb.gscbS.', now(), 'USER', now(), now()),
       ('d0000000-0000-7000-8000-000000000103', 'Nico Ríos', 'nico.demo@example.com', NULL,
        '{bcrypt}$2a$10$U3eVOWb9sQLYAn/MTySdPO1X/a.5XJFuORdyrb7ved5/Qb.gscbS.', now(), 'USER', now(), now()),
       ('d0000000-0000-7000-8000-000000000104', 'Juli Sosa', 'juli.demo@example.com', NULL,
        '{bcrypt}$2a$10$U3eVOWb9sQLYAn/MTySdPO1X/a.5XJFuORdyrb7ved5/Qb.gscbS.', now(), 'USER', now(), now()),
       ('d0000000-0000-7000-8000-000000000105', 'Sofi Lagos', 'sofi.demo@example.com', NULL,
        '{bcrypt}$2a$10$U3eVOWb9sQLYAn/MTySdPO1X/a.5XJFuORdyrb7ved5/Qb.gscbS.', now(), 'USER', now(), now()),
       ('d0000000-0000-7000-8000-000000000106', 'Camila Duarte', 'cliente.demo@example.com', '1155550101',
        '{bcrypt}$2a$10$U3eVOWb9sQLYAn/MTySdPO1X/a.5XJFuORdyrb7ved5/Qb.gscbS.', now(), 'USER', now(), now())
ON CONFLICT (id) DO UPDATE
    SET name = EXCLUDED.name, email = EXCLUDED.email, phone = EXCLUDED.phone,
        password_hash = EXCLUDED.password_hash, email_verified_at = EXCLUDED.email_verified_at,
        updated_at = now(), version = user_account.version + 1;

INSERT INTO business (id, name, slug, category, description, searchable, created_at, updated_at)
VALUES ('d0000000-0000-7000-8000-000000000001', 'Barbería El Faro', 'barberia-el-faro', 'BARBERSHOP',
        'Barbería de barrio con dos locales. Cortes clásicos y modernos, barba con toalla caliente y color. '
            || 'Es un negocio de ejemplo: entrá al panel con las cuentas de prueba y reservá sin miedo.',
        true, now(), now());
INSERT INTO business_slug (slug, business_id, claimed_at)
VALUES ('barberia-el-faro', 'd0000000-0000-7000-8000-000000000001', now());

INSERT INTO branch (id, business_id, name, street, neighborhood, city, phone, time_zone, created_at, updated_at)
VALUES ('d0000000-0000-7000-8000-000000000011', 'd0000000-0000-7000-8000-000000000001', 'Palermo',
        'Honduras 4800', 'Palermo', 'CABA', '1145550100', 'America/Argentina/Buenos_Aires', now(), now()),
       ('d0000000-0000-7000-8000-000000000012', 'd0000000-0000-7000-8000-000000000001', 'Belgrano',
        'Av. Cabildo 2200', 'Belgrano', 'CABA', '1145550200', 'America/Argentina/Buenos_Aires', now(), now());

-- Palermo de lunes a sábado de 10 a 20; Belgrano de martes a sábado de 10 a 19.
INSERT INTO branch_opening (id, business_id, branch_id, weekday, starts_at, ends_at)
SELECT gen_random_uuid(), 'd0000000-0000-7000-8000-000000000001'::UUID, 'd0000000-0000-7000-8000-000000000011'::UUID, d, '10:00'::TIME, '20:00'::TIME
FROM generate_series(1, 6) AS d
UNION ALL
SELECT gen_random_uuid(), 'd0000000-0000-7000-8000-000000000001'::UUID, 'd0000000-0000-7000-8000-000000000012'::UUID, d, '10:00'::TIME, '19:00'::TIME
FROM generate_series(2, 6) AS d;

INSERT INTO membership (id, user_id, business_id, role, created_at)
VALUES ('d0000000-0000-7000-8000-000000000201', 'd0000000-0000-7000-8000-000000000101', 'd0000000-0000-7000-8000-000000000001', 'OWNER', now()),
       ('d0000000-0000-7000-8000-000000000202', 'd0000000-0000-7000-8000-000000000102', 'd0000000-0000-7000-8000-000000000001', 'MANAGER', now()),
       ('d0000000-0000-7000-8000-000000000203', 'd0000000-0000-7000-8000-000000000103', 'd0000000-0000-7000-8000-000000000001', 'BARBER', now()),
       ('d0000000-0000-7000-8000-000000000204', 'd0000000-0000-7000-8000-000000000104', 'd0000000-0000-7000-8000-000000000001', 'BARBER', now()),
       ('d0000000-0000-7000-8000-000000000205', 'd0000000-0000-7000-8000-000000000105', 'd0000000-0000-7000-8000-000000000001', 'BARBER', now());
INSERT INTO membership_branch (membership_id, branch_id)
VALUES ('d0000000-0000-7000-8000-000000000202', 'd0000000-0000-7000-8000-000000000011'),
       ('d0000000-0000-7000-8000-000000000202', 'd0000000-0000-7000-8000-000000000012'),
       ('d0000000-0000-7000-8000-000000000203', 'd0000000-0000-7000-8000-000000000011'),
       ('d0000000-0000-7000-8000-000000000204', 'd0000000-0000-7000-8000-000000000011'),
       ('d0000000-0000-7000-8000-000000000205', 'd0000000-0000-7000-8000-000000000012');

INSERT INTO booking_settings (business_id, notice_hours, updated_at)
VALUES ('d0000000-0000-7000-8000-000000000001', 2, now());
INSERT INTO schedule_settings (business_id, buffer_minutes, min_notice_minutes, max_advance_days, slot_step_minutes, updated_at)
VALUES ('d0000000-0000-7000-8000-000000000001', 0, 30, 30, 15, now());
INSERT INTO notification_settings (business_id, reminder_hours, updated_at)
VALUES ('d0000000-0000-7000-8000-000000000001', '{24}', now());

INSERT INTO service (id, business_id, name, category, description, base_duration, base_price, price_min, price_max, status, created_at, updated_at)
VALUES ('d0000000-0000-7000-8000-000000000301', 'd0000000-0000-7000-8000-000000000001', 'Corte', 'Cortes',
        'Corte a tijera o máquina, con terminación a navaja.', 30, 9000, 8000, 12000, 'ACTIVE', now(), now()),
       ('d0000000-0000-7000-8000-000000000302', 'd0000000-0000-7000-8000-000000000001', 'Corte y lavado', 'Cortes',
        NULL, 45, 11000, NULL, NULL, 'ACTIVE', now(), now()),
       ('d0000000-0000-7000-8000-000000000303', 'd0000000-0000-7000-8000-000000000001', 'Barba', 'Barba',
        'Perfilado y toalla caliente.', 20, 6000, NULL, NULL, 'ACTIVE', now(), now()),
       ('d0000000-0000-7000-8000-000000000304', 'd0000000-0000-7000-8000-000000000001', 'Perfilado de cejas', 'Barba',
        NULL, 15, 4000, NULL, NULL, 'ACTIVE', now(), now()),
       ('d0000000-0000-7000-8000-000000000305', 'd0000000-0000-7000-8000-000000000001', 'Color', 'Color',
        'Tintura o reflejos.', 60, 18000, NULL, NULL, 'ACTIVE', now(), now());

INSERT INTO combo (id, business_id, name, active, created_at, updated_at)
VALUES ('d0000000-0000-7000-8000-000000000401', 'd0000000-0000-7000-8000-000000000001', 'Corte y barba', true, now(), now());
INSERT INTO combo_service (combo_id, position, service_id)
VALUES ('d0000000-0000-7000-8000-000000000401', 0, 'd0000000-0000-7000-8000-000000000301'),
       ('d0000000-0000-7000-8000-000000000401', 1, 'd0000000-0000-7000-8000-000000000303');

-- Servicios de cada profesional (precio y duración vacíos: los del negocio). Juli cobra el corte $ 10.000.
INSERT INTO barber_service (id, business_id, barber_id, service_id, price, duration, active, created_at, updated_at)
SELECT gen_random_uuid(), 'd0000000-0000-7000-8000-000000000001'::UUID, barber::UUID, service::UUID, price, NULL, true, now(), now()
FROM (VALUES ('d0000000-0000-7000-8000-000000000103', 'd0000000-0000-7000-8000-000000000301', NULL::NUMERIC),
             ('d0000000-0000-7000-8000-000000000103', 'd0000000-0000-7000-8000-000000000302', NULL),
             ('d0000000-0000-7000-8000-000000000103', 'd0000000-0000-7000-8000-000000000303', NULL),
             ('d0000000-0000-7000-8000-000000000104', 'd0000000-0000-7000-8000-000000000301', 10000),
             ('d0000000-0000-7000-8000-000000000104', 'd0000000-0000-7000-8000-000000000303', NULL),
             ('d0000000-0000-7000-8000-000000000104', 'd0000000-0000-7000-8000-000000000305', NULL),
             ('d0000000-0000-7000-8000-000000000105', 'd0000000-0000-7000-8000-000000000301', NULL),
             ('d0000000-0000-7000-8000-000000000105', 'd0000000-0000-7000-8000-000000000303', NULL),
             ('d0000000-0000-7000-8000-000000000105', 'd0000000-0000-7000-8000-000000000304', NULL))
         AS offer(barber, service, price);

INSERT INTO barber_profile (id, business_id, barber_id, bio, specialties, photo_url, updated_at)
VALUES (gen_random_uuid(), 'd0000000-0000-7000-8000-000000000001', 'd0000000-0000-7000-8000-000000000103',
        'Diez años con la máquina en la mano. Fan de los degradados prolijos.', '{Fade,Barba}', NULL, now()),
       (gen_random_uuid(), 'd0000000-0000-7000-8000-000000000001', 'd0000000-0000-7000-8000-000000000104',
        'Colorista. Si querés probar algo distinto, hablemos.', '{Color,Tijera}', NULL, now()),
       (gen_random_uuid(), 'd0000000-0000-7000-8000-000000000001', 'd0000000-0000-7000-8000-000000000105',
        'Barba clásica y perfilado de cejas.', '{Barba,Cejas}', NULL, now());

-- Horarios: Nico en Palermo de lunes a sábado (con corte al mediodía), Juli en Palermo de lunes a viernes y
-- Sofi en Belgrano de martes a sábado.
INSERT INTO work_shift (id, business_id, barber_id, branch_id, weekday, starts_at, ends_at)
SELECT gen_random_uuid(), 'd0000000-0000-7000-8000-000000000001'::UUID, barber::UUID, branch::UUID, d, starts::TIME, ends::TIME
FROM (VALUES ('d0000000-0000-7000-8000-000000000103', 'd0000000-0000-7000-8000-000000000011', 1, 6, '10:00', '14:00'),
             ('d0000000-0000-7000-8000-000000000103', 'd0000000-0000-7000-8000-000000000011', 1, 6, '15:00', '20:00'),
             ('d0000000-0000-7000-8000-000000000104', 'd0000000-0000-7000-8000-000000000011', 1, 5, '11:00', '19:00'),
             ('d0000000-0000-7000-8000-000000000105', 'd0000000-0000-7000-8000-000000000012', 2, 6, '10:00', '18:00'))
         AS shift(barber, branch, first_day, last_day, starts, ends)
         CROSS JOIN LATERAL generate_series(first_day, last_day) AS d;

INSERT INTO customer (id, business_id, user_id, name, email, phone, notes, preferences, created_at, updated_at)
VALUES ('d0000000-0000-7000-8000-000000000501', 'd0000000-0000-7000-8000-000000000001', 'd0000000-0000-7000-8000-000000000106',
        'Camila Duarte', 'cliente.demo@example.com', '1155550101', NULL, 'Prefiere turnos a la tarde.', now(), now()),
       ('d0000000-0000-7000-8000-000000000502', 'd0000000-0000-7000-8000-000000000001', NULL,
        'Tomás Acosta', NULL, '1155550102', 'Viene cada 3 semanas.', 'Degradado bajo, nada arriba.', now(), now()),
       ('d0000000-0000-7000-8000-000000000503', 'd0000000-0000-7000-8000-000000000001', NULL,
        'Valentina Ruiz', 'valen.demo@example.com', NULL, NULL, NULL, now(), now()),
       ('d0000000-0000-7000-8000-000000000504', 'd0000000-0000-7000-8000-000000000001', NULL,
        'Martín Gómez', NULL, '1155550104', 'Faltó una vez sin avisar.', NULL, now(), now()),
       ('d0000000-0000-7000-8000-000000000505', 'd0000000-0000-7000-8000-000000000001', NULL,
        'Lautaro Pérez', 'lautaro.demo@example.com', '1155550105', NULL, 'Barba con toalla caliente.', now(), now()),
       ('d0000000-0000-7000-8000-000000000506', 'd0000000-0000-7000-8000-000000000001', NULL,
        'Agustina Medina', 'agus.demo@example.com', NULL, NULL, NULL, now(), now()),
       ('d0000000-0000-7000-8000-000000000507', 'd0000000-0000-7000-8000-000000000001', NULL,
        'Joaquín Díaz', NULL, '1155550107', NULL, NULL, now(), now()),
       ('d0000000-0000-7000-8000-000000000508', 'd0000000-0000-7000-8000-000000000001', NULL,
        'Florencia Castro', 'flor.demo@example.com', '1155550108', NULL, 'Cejas finitas.', now(), now());

-- Turnos de la semana pasada (terminados, uno sin aviso) y de los próximos días. Cada línea es un servicio
-- (el combo tiene dos) y el precio sale de lo que cobra ese profesional.
CREATE TEMPORARY TABLE demo_plan (
    n        INTEGER PRIMARY KEY,
    barber   UUID,
    branch   UUID,
    starts   TIMESTAMPTZ,
    customer UUID,
    status   VARCHAR(20),
    source   VARCHAR(20),
    combo    UUID
) ON COMMIT DROP;
CREATE TEMPORARY TABLE demo_plan_line (n INTEGER, position INTEGER, service UUID) ON COMMIT DROP;

INSERT INTO demo_plan
SELECT n, barber::UUID, branch::UUID, pg_temp.demo_at(day_offset, at::TIME), ('d0000000-0000-7000-8000-00000000050' || c)::UUID,
       status, source, combo::UUID
FROM (VALUES
          -- Nico, Palermo
          (1, 'd0000000-0000-7000-8000-000000000103', 'd0000000-0000-7000-8000-000000000011', -6, '10:00', 2, 'COMPLETED', 'WEB', NULL),
          (2, 'd0000000-0000-7000-8000-000000000103', 'd0000000-0000-7000-8000-000000000011', -3, '11:00', 3, 'COMPLETED', 'COUNTER', NULL),
          (3, 'd0000000-0000-7000-8000-000000000103', 'd0000000-0000-7000-8000-000000000011', -1, '12:00', 4, 'NO_SHOW', 'WEB', NULL),
          (4, 'd0000000-0000-7000-8000-000000000103', 'd0000000-0000-7000-8000-000000000011', 0, '10:30', 1, 'CONFIRMED', 'WEB', NULL),
          (5, 'd0000000-0000-7000-8000-000000000103', 'd0000000-0000-7000-8000-000000000011', 0, '16:00', 5, 'CONFIRMED', 'WEB', 'd0000000-0000-7000-8000-000000000401'),
          (6, 'd0000000-0000-7000-8000-000000000103', 'd0000000-0000-7000-8000-000000000011', 1, '17:00', 6, 'CONFIRMED', 'WEB', NULL),
          (7, 'd0000000-0000-7000-8000-000000000103', 'd0000000-0000-7000-8000-000000000011', 2, '18:00', 7, 'PENDING', 'COUNTER', NULL),
          (8, 'd0000000-0000-7000-8000-000000000103', 'd0000000-0000-7000-8000-000000000011', 4, '13:00', 8, 'CONFIRMED', 'WEB', NULL),
          -- Juli, Palermo
          (9, 'd0000000-0000-7000-8000-000000000104', 'd0000000-0000-7000-8000-000000000011', -5, '11:30', 3, 'COMPLETED', 'WEB', NULL),
          (10, 'd0000000-0000-7000-8000-000000000104', 'd0000000-0000-7000-8000-000000000011', -2, '14:00', 6, 'COMPLETED', 'WEB', NULL),
          (11, 'd0000000-0000-7000-8000-000000000104', 'd0000000-0000-7000-8000-000000000011', 0, '12:30', 2, 'CONFIRMED', 'WEB', NULL),
          (12, 'd0000000-0000-7000-8000-000000000104', 'd0000000-0000-7000-8000-000000000011', 1, '15:30', 4, 'CANCELLED', 'WEB', NULL),
          (13, 'd0000000-0000-7000-8000-000000000104', 'd0000000-0000-7000-8000-000000000011', 3, '16:30', 7, 'CONFIRMED', 'WEB', NULL),
          -- Sofi, Belgrano
          (14, 'd0000000-0000-7000-8000-000000000105', 'd0000000-0000-7000-8000-000000000012', -4, '10:15', 8, 'COMPLETED', 'COUNTER', NULL),
          (15, 'd0000000-0000-7000-8000-000000000105', 'd0000000-0000-7000-8000-000000000012', 0, '11:15', 5, 'CONFIRMED', 'WEB', NULL),
          (16, 'd0000000-0000-7000-8000-000000000105', 'd0000000-0000-7000-8000-000000000012', 2, '14:45', 1, 'CONFIRMED', 'WEB', NULL))
         AS plan(n, barber, branch, day_offset, at, c, status, source, combo);

INSERT INTO demo_plan_line (n, position, service)
SELECT n, 0, service::UUID
FROM (VALUES (1, 'd0000000-0000-7000-8000-000000000301'), (2, 'd0000000-0000-7000-8000-000000000303'),
             (3, 'd0000000-0000-7000-8000-000000000301'), (4, 'd0000000-0000-7000-8000-000000000301'),
             (5, 'd0000000-0000-7000-8000-000000000301'), (6, 'd0000000-0000-7000-8000-000000000302'),
             (7, 'd0000000-0000-7000-8000-000000000301'), (8, 'd0000000-0000-7000-8000-000000000303'),
             (9, 'd0000000-0000-7000-8000-000000000305'), (10, 'd0000000-0000-7000-8000-000000000301'),
             (11, 'd0000000-0000-7000-8000-000000000305'), (12, 'd0000000-0000-7000-8000-000000000301'),
             (13, 'd0000000-0000-7000-8000-000000000301'), (14, 'd0000000-0000-7000-8000-000000000304'),
             (15, 'd0000000-0000-7000-8000-000000000301'), (16, 'd0000000-0000-7000-8000-000000000303'))
         AS line(n, service)
UNION ALL
SELECT 5, 1, 'd0000000-0000-7000-8000-000000000303'::UUID;

CREATE TEMPORARY VIEW demo_priced_line AS
SELECT l.n, l.position, s.id AS service, s.name,
       COALESCE(bs.price, s.base_price) AS price,
       COALESCE(bs.duration, s.base_duration) AS duration
FROM demo_plan_line l
         JOIN demo_plan p ON p.n = l.n
         JOIN service s ON s.id = l.service
         LEFT JOIN barber_service bs ON bs.barber_id = p.barber AND bs.service_id = s.id;

INSERT INTO appointment (id, business_id, branch_id, barber_id, customer_id, combo_id, status, source, starts_at,
                         ends_at, total_price, created_at, updated_at)
SELECT ('d0000000-0000-7000-8000-' || lpad((600 + p.n)::TEXT, 12, '0'))::UUID,
       'd0000000-0000-7000-8000-000000000001'::UUID, p.branch, p.barber, p.customer, p.combo, p.status, p.source, p.starts,
       p.starts + make_interval(mins => totals.minutes), totals.price, p.starts - INTERVAL '3 days', now()
FROM demo_plan p
         JOIN (SELECT n, SUM(duration)::INTEGER AS minutes, SUM(price) AS price FROM demo_priced_line GROUP BY n) totals
              ON totals.n = p.n;

INSERT INTO appointment_line (appointment_id, position, service_id, service_name, price, duration_minutes)
SELECT ('d0000000-0000-7000-8000-' || lpad((600 + n)::TEXT, 12, '0'))::UUID, position, service, name, price, duration
FROM demo_priced_line;

DROP VIEW demo_priced_line;

-- Avisos sin leer para Nico, así la campanita del panel tiene algo que mostrar.
INSERT INTO app_notice (id, business_id, user_id, appointment_id, type, message, created_at)
VALUES (gen_random_uuid(), 'd0000000-0000-7000-8000-000000000001', 'd0000000-0000-7000-8000-000000000103',
        'd0000000-0000-7000-8000-000000000605', 'APPOINTMENT_BOOKED',
        'Nuevo turno: Corte y barba con Lautaro Pérez.', now() - INTERVAL '2 hours'),
       (gen_random_uuid(), 'd0000000-0000-7000-8000-000000000001', 'd0000000-0000-7000-8000-000000000103',
        'd0000000-0000-7000-8000-000000000606', 'APPOINTMENT_BOOKED',
        'Nuevo turno: Corte y lavado con Agustina Medina.', now() - INTERVAL '1 hour');
