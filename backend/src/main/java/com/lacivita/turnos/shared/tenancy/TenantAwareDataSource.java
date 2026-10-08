package com.lacivita.turnos.shared.tenancy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Antes de entregar cada conexión del pool, informa a PostgreSQL el contexto actual (negocio, persona y
 * si es una operación de sistema). Las políticas de Row Level Security de la migración V8 usan esos
 * valores.
 *
 * <p>Se escriben siempre los tres valores, así una conexión reutilizada nunca conserva el contexto de la
 * solicitud anterior.
 *
 * <p>Solo lee {@link TenantContext}, nunca la seguridad: el contexto de seguridad se carga de forma
 * diferida desde la sesión guardada en PostgreSQL, y leerlo acá pedía otra conexión en cadena hasta
 * agotar el pool.
 */
class TenantAwareDataSource extends DelegatingDataSource {

    private static final String APPLY_CONTEXT = """
            SELECT set_config('app.business_id', ?, false),
                   set_config('app.user_id', ?, false),
                   set_config('app.rls_bypass', ?, false)
            """;

    TenantAwareDataSource(DataSource target) {
        super(target);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return withContext(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return withContext(super.getConnection(username, password));
    }

    private static Connection withContext(Connection connection) throws SQLException {
        try (var statement = connection.prepareStatement(APPLY_CONTEXT)) {
            statement.setString(1, asSetting(TenantContext.currentBusiness()));
            statement.setString(2, asSetting(TenantContext.currentUser()));
            statement.setString(3, TenantContext.isSystem() ? "on" : "off");
            statement.execute();
            return connection;
        } catch (SQLException ex) {
            connection.close();
            throw ex;
        }
    }

    private static String asSetting(Optional<UUID> id) {
        return id.map(UUID::toString).orElse("");
    }
}
