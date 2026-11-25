-- AKRON - Security roles and supporting indexes
-- Run after 06_compliance_schemas.sql and 07_compliance_reports_audit.sql.
-- Set the akron_app password separately in pgAdmin or with psql \password.
\set ON_ERROR_STOP on

-- Roles are cluster-level objects; create them only when absent.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'akron_readonly') THEN
        CREATE ROLE akron_readonly NOLOGIN;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'akron_operator') THEN
        CREATE ROLE akron_operator NOLOGIN;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'akron_admin') THEN
        CREATE ROLE akron_admin NOLOGIN;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'akron_app') THEN
        CREATE ROLE akron_app LOGIN INHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE;
    END IF;
END
$;

-- Normalize attributes even if the login role existed before this migration.
ALTER ROLE akron_app WITH LOGIN INHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE;

GRANT CONNECT ON DATABASE akron_db TO akron_app;

GRANT USAGE ON SCHEMA app, reportes TO akron_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA app TO akron_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA reportes TO akron_readonly;

GRANT USAGE ON SCHEMA app, reportes TO akron_operator;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA app TO akron_operator;
GRANT SELECT ON ALL TABLES IN SCHEMA reportes TO akron_operator;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA app TO akron_operator;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA app, reportes, audit TO akron_operator;
GRANT EXECUTE ON ALL PROCEDURES IN SCHEMA app, reportes, audit TO akron_operator;

GRANT USAGE ON SCHEMA app, reportes, audit TO akron_admin;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA app, audit TO akron_admin;
GRANT SELECT ON ALL TABLES IN SCHEMA reportes TO akron_admin;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA app, audit TO akron_admin;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA app, reportes, audit TO akron_admin;
GRANT EXECUTE ON ALL PROCEDURES IN SCHEMA app, reportes, audit TO akron_admin;

-- The application login inherits the operational and read-only permissions.
GRANT akron_operator, akron_readonly TO akron_app;
GRANT USAGE ON SCHEMA app, reportes TO akron_app;

-- The validator requires that PUBLIC has no direct privileges on app tables.
REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA app FROM PUBLIC;

-- Default schema resolution preserves Java's unqualified SQL statements.
ALTER ROLE akron_app IN DATABASE akron_db SET search_path TO app, public;

-- Indexes for foreign-key columns not already leading an existing index.
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON app.user_roles(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission_id ON app.role_permissions(permission_id);
CREATE INDEX IF NOT EXISTS idx_user_permissions_permission_id ON app.user_permissions(permission_id);
CREATE INDEX IF NOT EXISTS idx_sales_payment_method_id ON app.sales(payment_method_id);
CREATE INDEX IF NOT EXISTS idx_inventory_movements_sale_detail_id
    ON app.inventory_movements(sale_detail_id);

-- Ensure the runtime role can use the application schema and sequences.
GRANT USAGE ON SCHEMA app TO akron_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA app TO akron_app;

-- IMPORTANT: set a strong password outside version control before configuring
-- AKRON_DB_USER=akron_app and AKRON_DB_PASSWORD in the runtime environment.
