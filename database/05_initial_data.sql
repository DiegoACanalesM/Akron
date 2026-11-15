-- Ejecutar después de crear tablas. No insertar usuarios ni contraseñas aquí.

INSERT INTO roles (name, description) VALUES
    ('OWNER', 'Propietario y administrador del negocio'),
    ('EMPLOYEE', 'Empleado del negocio')
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (code, description) VALUES
    ('USERS_MANAGE', 'Administrar usuarios'),
    ('ROLES_MANAGE', 'Administrar roles y permisos'),
    ('CATEGORIES_VIEW', 'Consultar categorías'),
    ('CATEGORIES_MANAGE', 'Administrar categorías'),
    ('PRODUCTS_VIEW', 'Consultar productos'),
    ('PRODUCTS_MANAGE', 'Administrar productos'),
    ('INVENTORY_VIEW', 'Consultar existencias y movimientos'),
    ('INVENTORY_MANAGE', 'Registrar entradas, salidas y ajustes'),
    ('CUSTOMERS_VIEW', 'Consultar clientes'),
    ('CUSTOMERS_MANAGE', 'Administrar clientes'),
    ('SALES_VIEW', 'Consultar ventas'),
    ('SALES_CREATE', 'Registrar ventas'),
    ('SALES_CANCEL', 'Cancelar ventas y revertir existencias'),
    ('ANALYTICS_VIEW', 'Consultar indicadores y reportes')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'OWNER'
ON CONFLICT DO NOTHING;

-- EMPLOYEE comienza sin permisos; OWNER asigna los necesarios.

INSERT INTO payment_methods (name) VALUES
    ('CASH'), ('CARD'), ('BANK_TRANSFER'), ('OTHER')
ON CONFLICT (name) DO NOTHING;
