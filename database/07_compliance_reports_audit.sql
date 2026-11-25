-- AKRON - Reports, PostgreSQL routines and audit trail
-- Run after 06_compliance_schemas.sql.
\set ON_ERROR_STOP on

BEGIN;

CREATE OR REPLACE VIEW reportes.vw_customer_purchase_summary AS
SELECT
    c.id AS customer_id,
    c.name AS customer_name,
    COUNT(DISTINCT s.id) FILTER (WHERE s.status = 'COMPLETED') AS completed_sales,
    COALESCE(SUM(sd.subtotal) FILTER (WHERE s.status = 'COMPLETED'), 0)::NUMERIC(14, 2)
        AS total_purchases
FROM app.customers c
LEFT JOIN app.sales s ON s.customer_id = c.id
LEFT JOIN app.sale_details sd ON sd.sale_id = s.id
GROUP BY c.id, c.name;

COMMENT ON VIEW reportes.vw_customer_purchase_summary IS
    'Resumen de cantidad de ventas completadas y valor comprado por cliente.';

CREATE OR REPLACE FUNCTION app.fn_stock_disponible(p_product_id BIGINT)
RETURNS INTEGER
LANGUAGE SQL
STABLE
AS $$
    SELECT p.stock
    FROM app.products p
    WHERE p.id = p_product_id
$$;

CREATE OR REPLACE FUNCTION reportes.fn_total_ventas(p_desde DATE, p_hasta DATE)
RETURNS NUMERIC(14, 2)
LANGUAGE SQL
STABLE
AS $$
    SELECT COALESCE(SUM(sd.subtotal), 0)::NUMERIC(14, 2)
    FROM app.sales s
    JOIN app.sale_details sd ON sd.sale_id = s.id
    WHERE s.status = 'COMPLETED'
      AND (s.sale_date AT TIME ZONE 'America/Bogota')::DATE
          BETWEEN p_desde AND p_hasta
$$;

CREATE TABLE IF NOT EXISTS audit.audit_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    schema_name TEXT NOT NULL,
    table_name TEXT NOT NULL,
    record_id TEXT,
    operation TEXT NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    old_data JSONB,
    new_data JSONB,
    database_user TEXT NOT NULL DEFAULT CURRENT_USER,
    application_name TEXT,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE audit.audit_log IS
    'Auditoría técnica de inserciones, actualizaciones y eliminaciones en tablas operacionales.';

CREATE OR REPLACE FUNCTION audit.fn_audit_row_change()
RETURNS TRIGGER
LANGUAGE PLPGSQL
SECURITY DEFINER
SET search_path = pg_catalog, audit
AS $audit$
DECLARE
    v_old JSONB;
    v_new JSONB;
    v_record_id TEXT;
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        v_old := to_jsonb(OLD);
        v_record_id := v_old ->> 'id';
    END IF;

    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        v_new := to_jsonb(NEW);
        v_record_id := v_new ->> 'id';
    END IF;

    INSERT INTO audit.audit_log (
        schema_name, table_name, record_id, operation,
        old_data, new_data, database_user, application_name
    )
    VALUES (
        TG_TABLE_SCHEMA, TG_TABLE_NAME, v_record_id, TG_OP,
        v_old, v_new, SESSION_USER, current_setting('application_name', TRUE)
    );

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$audit$;

-- This trigger checks that the movement's final stock matches the product row.
-- Java already updates product stock and inserts the movement in the same transaction.
CREATE OR REPLACE FUNCTION app.fn_validate_inventory_movement()
RETURNS TRIGGER
LANGUAGE PLPGSQL
AS $$
DECLARE
    v_current_stock INTEGER;
BEGIN
    SELECT p.stock
      INTO v_current_stock
      FROM app.products p
     WHERE p.id = NEW.product_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Product % does not exist', NEW.product_id;
    END IF;

    IF v_current_stock <> NEW.stock_after THEN
        RAISE EXCEPTION
            'Inventory movement stock_after (%) does not match current product stock (%)',
            NEW.stock_after, v_current_stock;
    END IF;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE PROCEDURE app.sp_set_product_active(
    p_product_id BIGINT,
    p_active BOOLEAN
)
LANGUAGE PLPGSQL
AS $$
BEGIN
    IF p_product_id IS NULL OR p_active IS NULL THEN
        RAISE EXCEPTION 'Product ID and active state are required';
    END IF;

    UPDATE app.products
       SET is_active = p_active
     WHERE id = p_product_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Product % was not found', p_product_id;
    END IF;
END;
$$;

DROP TRIGGER IF EXISTS trg_audit_sales ON app.sales;
CREATE TRIGGER trg_audit_sales
AFTER INSERT OR UPDATE OR DELETE ON app.sales
FOR EACH ROW EXECUTE FUNCTION audit.fn_audit_row_change();

DROP TRIGGER IF EXISTS trg_audit_inventory_movements ON app.inventory_movements;
CREATE TRIGGER trg_audit_inventory_movements
AFTER INSERT OR UPDATE OR DELETE ON app.inventory_movements
FOR EACH ROW EXECUTE FUNCTION audit.fn_audit_row_change();

DROP TRIGGER IF EXISTS trg_validate_inventory_movement ON app.inventory_movements;
CREATE TRIGGER trg_validate_inventory_movement
BEFORE INSERT ON app.inventory_movements
FOR EACH ROW EXECUTE FUNCTION app.fn_validate_inventory_movement();

COMMIT;
