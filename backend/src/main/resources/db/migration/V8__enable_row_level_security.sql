-- Aislamiento entre negocios con Row Level Security (segunda barrera, después del filtro de Hibernate).
--
-- La aplicación se conecta con el rol ${app_role}, que no es dueño de las tablas ni superusuario: por
-- eso PostgreSQL le aplica estas políticas. Las migraciones corren con el rol dueño, que no las sufre.
--
-- En cada conexión la aplicación informa el contexto con variables de sesión:
--   app.business_id  negocio sobre el que se está operando (vacío si ninguno)
--   app.user_id      persona con sesión iniciada (vacío si ninguna)
--   app.rls_bypass   'on' solo en operaciones de sistema explícitas y justificadas
-- Sin contexto, las tablas protegidas no devuelven filas: ante un olvido, falla cerrado.

CREATE FUNCTION app_current_business() RETURNS UUID
    LANGUAGE sql STABLE
AS $$ SELECT NULLIF(current_setting('app.business_id', true), '')::UUID $$;

CREATE FUNCTION app_current_user() RETURNS UUID
    LANGUAGE sql STABLE
AS $$ SELECT NULLIF(current_setting('app.user_id', true), '')::UUID $$;

CREATE FUNCTION app_rls_bypassed() RETURNS BOOLEAN
    LANGUAGE sql STABLE
AS $$ SELECT COALESCE(current_setting('app.rls_bypass', true), '') = 'on' $$;

-- Permisos del rol de la aplicación: solo datos, nunca estructura.
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO ${app_role};
REVOKE ALL ON TABLE ${flyway:table} FROM ${app_role};
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ${app_role};

-- Negocios, slugs y sucursales: la lectura es pública (es la información de la página de reservas);
-- solo se modifica dentro del negocio.
ALTER TABLE business ENABLE ROW LEVEL SECURITY;
CREATE POLICY business_read ON business FOR SELECT USING (true);
CREATE POLICY business_insert ON business FOR INSERT
    WITH CHECK (app_rls_bypassed() OR id = app_current_business());
CREATE POLICY business_update ON business FOR UPDATE
    USING (app_rls_bypassed() OR id = app_current_business())
    WITH CHECK (app_rls_bypassed() OR id = app_current_business());
CREATE POLICY business_delete ON business FOR DELETE
    USING (app_rls_bypassed() OR id = app_current_business());

ALTER TABLE business_slug ENABLE ROW LEVEL SECURITY;
CREATE POLICY business_slug_read ON business_slug FOR SELECT USING (true);
CREATE POLICY business_slug_write ON business_slug FOR INSERT
    WITH CHECK (app_rls_bypassed() OR business_id = app_current_business());
CREATE POLICY business_slug_delete ON business_slug FOR DELETE
    USING (app_rls_bypassed() OR business_id = app_current_business());

ALTER TABLE branch ENABLE ROW LEVEL SECURITY;
CREATE POLICY branch_read ON branch FOR SELECT USING (true);
CREATE POLICY branch_insert ON branch FOR INSERT
    WITH CHECK (app_rls_bypassed() OR business_id = app_current_business());
CREATE POLICY branch_update ON branch FOR UPDATE
    USING (app_rls_bypassed() OR business_id = app_current_business())
    WITH CHECK (app_rls_bypassed() OR business_id = app_current_business());
CREATE POLICY branch_delete ON branch FOR DELETE
    USING (app_rls_bypassed() OR business_id = app_current_business());

-- Membresías: dentro del negocio se ven todas; además cada persona ve las propias (para listar sus
-- negocios y para los chequeos de permisos). Solo se modifican dentro del negocio.
ALTER TABLE membership ENABLE ROW LEVEL SECURITY;
CREATE POLICY membership_read ON membership FOR SELECT
    USING (app_rls_bypassed() OR business_id = app_current_business() OR user_id = app_current_user());
CREATE POLICY membership_insert ON membership FOR INSERT
    WITH CHECK (app_rls_bypassed() OR business_id = app_current_business());
CREATE POLICY membership_update ON membership FOR UPDATE
    USING (app_rls_bypassed() OR business_id = app_current_business())
    WITH CHECK (app_rls_bypassed() OR business_id = app_current_business());
CREATE POLICY membership_delete ON membership FOR DELETE
    USING (app_rls_bypassed() OR business_id = app_current_business());

-- Las tablas hijas heredan la visibilidad de su fila padre (la subconsulta ya pasa por su política).
ALTER TABLE membership_branch ENABLE ROW LEVEL SECURITY;
CREATE POLICY membership_branch_access ON membership_branch
    USING (EXISTS (SELECT 1 FROM membership m WHERE m.id = membership_branch.membership_id))
    WITH CHECK (EXISTS (SELECT 1 FROM membership m WHERE m.id = membership_branch.membership_id));

ALTER TABLE invitation ENABLE ROW LEVEL SECURITY;
CREATE POLICY invitation_access ON invitation
    USING (app_rls_bypassed() OR business_id = app_current_business())
    WITH CHECK (app_rls_bypassed() OR business_id = app_current_business());

ALTER TABLE invitation_branch ENABLE ROW LEVEL SECURITY;
CREATE POLICY invitation_branch_access ON invitation_branch
    USING (EXISTS (SELECT 1 FROM invitation i WHERE i.id = invitation_branch.invitation_id))
    WITH CHECK (EXISTS (SELECT 1 FROM invitation i WHERE i.id = invitation_branch.invitation_id));

ALTER TABLE audit_log ENABLE ROW LEVEL SECURITY;
CREATE POLICY audit_log_access ON audit_log
    USING (app_rls_bypassed() OR business_id = app_current_business())
    WITH CHECK (app_rls_bypassed() OR business_id = app_current_business());
