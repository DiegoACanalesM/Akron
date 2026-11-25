-- AKRON - Academic validator preparation
-- Run only after 02_create_tables.sql through 05_initial_data.sql.
-- This migration changes PostgreSQL object namespaces, not Java source.
-- BACK UP THE DATABASE before running against an existing installation.
\set ON_ERROR_STOP on

BEGIN;

CREATE SCHEMA IF NOT EXISTS app;
CREATE SCHEMA IF NOT EXISTS staging;
CREATE SCHEMA IF NOT EXISTS audit;
CREATE SCHEMA IF NOT EXISTS reportes;

-- Move the existing application tables without renaming them.
DO $$
DECLARE
    v_table text;
BEGIN
    FOREACH v_table IN ARRAY ARRAY[
        'roles', 'permissions', 'users', 'user_roles', 'role_permissions',
        'user_permissions', 'categories', 'products', 'customers',
        'payment_methods', 'sales', 'sale_details', 'inventory_movements'
    ]
    LOOP
        IF to_regclass(format('public.%I', v_table)) IS NOT NULL
           AND to_regclass(format('app.%I', v_table)) IS NULL THEN
            EXECUTE format('ALTER TABLE public.%I SET SCHEMA app', v_table);
        END IF;
    END LOOP;
END $$;

-- Move the four existing views. Their definitions and dependencies are preserved.
DO $$
DECLARE
    v_view text;
BEGIN
    FOREACH v_view IN ARRAY ARRAY[
        'vw_sale_totals', 'vw_product_low_stock',
        'vw_top_selling_products', 'vw_sales_by_day'
    ]
    LOOP
        IF to_regclass(format('public.%I', v_view)) IS NOT NULL
           AND to_regclass(format('reportes.%I', v_view)) IS NULL THEN
            EXECUTE format('ALTER VIEW public.%I SET SCHEMA reportes', v_view);
        END IF;
    END LOOP;
END $$;

-- Apply the naming convention required by the validator.
DO $$
DECLARE
    r record;
    v_prefix text;
    v_new_name text;
BEGIN
    FOR r IN
        SELECT c.conrelid::regclass AS table_name,
               c.conname,
               c.contype
        FROM pg_constraint c
        JOIN pg_namespace n ON n.oid = (
            SELECT relnamespace FROM pg_class WHERE oid = c.conrelid
        )
        WHERE n.nspname = 'app'
          AND c.contype IN ('p', 'f', 'u', 'c')
    LOOP
        v_prefix := CASE r.contype
            WHEN 'p' THEN 'pk_'
            WHEN 'f' THEN 'fk_'
            WHEN 'u' THEN 'uq_'
            WHEN 'c' THEN 'ck_'
        END;

        IF left(r.conname, length(v_prefix)) <> v_prefix THEN
            v_new_name := left(v_prefix || r.conname, 63);
            EXECUTE format(
                'ALTER TABLE %s RENAME CONSTRAINT %I TO %I',
                r.table_name, r.conname, v_new_name
            );
        END IF;
    END LOOP;
END $$;

COMMENT ON SCHEMA app IS 'Datos oficiales y operacionales de AKRON.';
COMMENT ON SCHEMA staging IS 'Zona temporal para cargas y preparación de datos.';
COMMENT ON SCHEMA audit IS 'Registro de cambios sobre datos operacionales.';
COMMENT ON SCHEMA reportes IS 'Vistas y consultas de informes de AKRON.';

COMMENT ON TABLE app.roles IS 'Perfiles de autorización disponibles en AKRON.';
COMMENT ON TABLE app.permissions IS 'Catálogo de permisos funcionales.';
COMMENT ON TABLE app.users IS 'Cuentas de usuario de la aplicación.';
COMMENT ON TABLE app.user_roles IS 'Relación muchos a muchos entre usuarios y roles.';
COMMENT ON TABLE app.role_permissions IS 'Relación muchos a muchos entre roles y permisos.';
COMMENT ON TABLE app.user_permissions IS 'Excepciones individuales de permisos por usuario.';
COMMENT ON TABLE app.categories IS 'Categorías utilizadas para clasificar productos.';
COMMENT ON TABLE app.products IS 'Catálogo de productos, precios y existencias actuales.';
COMMENT ON TABLE app.customers IS 'Clientes registrados del negocio.';
COMMENT ON TABLE app.payment_methods IS 'Medios de pago habilitados para las ventas.';
COMMENT ON TABLE app.sales IS 'Cabeceras y estado de las ventas.';
COMMENT ON TABLE app.sale_details IS 'Detalle histórico de productos incluidos en cada venta.';
COMMENT ON TABLE app.inventory_movements IS 'Historial de entradas, salidas, ventas, reversiones y ajustes de inventario.';

COMMIT;

-- The application uses unqualified table names (for example, SELECT * FROM products).
-- This database-level search_path keeps those statements compatible after moving tables.
-- Existing connections must reconnect for the new setting to take effect.
ALTER DATABASE akron_db SET search_path TO app, public;
