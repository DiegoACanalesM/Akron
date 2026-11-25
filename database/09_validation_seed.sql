-- AKRON - Fictional dataset for the academic PostgreSQL validator.
-- Run only on a disposable evaluation database after scripts 01-08.
-- Requires at least one active user created through AKRON's secure setup flow.
-- Do NOT run against a production database or a database containing real customer data.
\set ON_ERROR_STOP on

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM app.users WHERE is_active = TRUE) THEN
        RAISE EXCEPTION
            'Create an active AKRON user first. This seed does not create passwords or accounts.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM app.products
        WHERE name LIKE 'DEMO-VALIDATION-PRODUCT-%'
    ) THEN
        RAISE EXCEPTION
            'Validation demo rows already exist. Restore a clean evaluation backup before reseeding.';
    END IF;
END
$$;

-- Ensure the user-role and user-permission bridge tables are not empty.
INSERT INTO app.user_roles(user_id, role_id)
SELECT u.id, r.id
FROM (SELECT id FROM app.users WHERE is_active = TRUE ORDER BY id LIMIT 1) u
CROSS JOIN app.roles r
WHERE r.name = 'EMPLOYEE'
  AND NOT EXISTS (
      SELECT 1 FROM app.user_roles ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO app.user_permissions(user_id, permission_id, granted)
SELECT u.id, p.id, FALSE
FROM (SELECT id FROM app.users WHERE is_active = TRUE ORDER BY id LIMIT 1) u
CROSS JOIN app.permissions p
WHERE p.code = 'ANALYTICS_VIEW'
  AND NOT EXISTS (
      SELECT 1 FROM app.user_permissions up
      WHERE up.user_id = u.id AND up.permission_id = p.id
  );

INSERT INTO app.categories(name, description, is_active)
SELECT
    'DEMO-CATEGORY-' || lpad(g::text, 3, '0'),
    'Categoría ficticia para validación académica',
    TRUE
FROM generate_series(1, 10) AS g
ON CONFLICT (name) DO NOTHING;

INSERT INTO app.customers(name, document, phone, address, is_active)
SELECT
    'Cliente de prueba ' || lpad(g::text, 3, '0'),
    'DEMO-UPC-' || lpad(g::text, 5, '0'),
    '300000' || lpad(g::text, 4, '0'),
    'Dirección ficticia ' || g,
    TRUE
FROM generate_series(1, 100) AS g
ON CONFLICT (document) DO NOTHING;

INSERT INTO app.products(
    name, description, price, stock, minimum_stock, category_id, is_active
)
SELECT
    'DEMO-VALIDATION-PRODUCT-' || lpad(g::text, 3, '0'),
    'Producto ficticio para validar el proyecto académico',
    (10000 + g * 125)::NUMERIC(12, 2),
    99,
    5,
    c.id,
    TRUE
FROM generate_series(1, 100) AS g
JOIN app.categories c
  ON c.name = 'DEMO-CATEGORY-' || lpad((((g - 1) % 10) + 1)::text, 3, '0');

-- Create 100 completed sales, each with one fictional customer and one product.
INSERT INTO app.sales(customer_id, user_id, payment_method_id, sale_date, status)
SELECT
    c.id,
    (SELECT id FROM app.users WHERE is_active = TRUE ORDER BY id LIMIT 1),
    (SELECT id FROM app.payment_methods WHERE is_active = TRUE ORDER BY id LIMIT 1),
    CURRENT_TIMESTAMP - (g * INTERVAL '1 day'),
    'COMPLETED'
FROM generate_series(1, 100) AS g
JOIN app.customers c
  ON c.document = 'DEMO-UPC-' || lpad(g::text, 5, '0');

INSERT INTO app.sale_details(
    sale_id, product_id, product_name, category_name, quantity, unit_price
)
SELECT
    s.id,
    p.id,
    p.name,
    c.name,
    1,
    p.price
FROM app.sales s
JOIN app.customers cu ON cu.id = s.customer_id
JOIN app.products p
  ON p.name = 'DEMO-VALIDATION-PRODUCT-' ||
     lpad(right(cu.document, 3), 3, '0')
JOIN app.categories c ON c.id = p.category_id
WHERE cu.document LIKE 'DEMO-UPC-%'
  AND NOT EXISTS (SELECT 1 FROM app.sale_details d WHERE d.sale_id = s.id);

-- The products were seeded with stock 99. Each corresponding sale movement
-- documents a fictional decrease from 100 to 99, matching the current stock.
INSERT INTO app.inventory_movements(
    product_id, user_id, sale_detail_id, movement_type,
    quantity, stock_before, stock_after, note
)
SELECT
    d.product_id,
    s.user_id,
    d.id,
    'SALE',
    d.quantity,
    100,
    99,
    'Movimiento ficticio de validación académica'
FROM app.sale_details d
JOIN app.sales s ON s.id = d.sale_id
JOIN app.customers c ON c.id = s.customer_id
WHERE c.document LIKE 'DEMO-UPC-%'
  AND NOT EXISTS (
      SELECT 1 FROM app.inventory_movements m
      WHERE m.sale_detail_id = d.id AND m.movement_type = 'SALE'
  );

COMMIT;

-- Expected: no empty app tables and at least 500 rows in app.
-- This script intentionally uses fictional data only.
