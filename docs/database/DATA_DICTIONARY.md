# AKRON - Diccionario de datos

Todos los identificadores son BIGINT GENERATED ALWAYS AS IDENTITY, excepto las tablas puente, que utilizan claves compuestas.

## Seguridad

### roles
- id BIGINT PK IDENTITY
- name VARCHAR(40) NOT NULL UNIQUE
- description VARCHAR(200)

### permissions
- id BIGINT PK IDENTITY
- code VARCHAR(80) NOT NULL UNIQUE
- description VARCHAR(200) NOT NULL

### users
- id BIGINT PK IDENTITY
- username VARCHAR(60) NOT NULL UNIQUE
- password_hash VARCHAR(255) NOT NULL
- full_name VARCHAR(150) NOT NULL
- is_active BOOLEAN NOT NULL DEFAULT TRUE
- created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP

### user_roles
- user_id BIGINT PK/FK users
- role_id BIGINT PK/FK roles

### role_permissions
- role_id BIGINT PK/FK roles
- permission_id BIGINT PK/FK permissions

### user_permissions
- user_id BIGINT PK/FK users
- permission_id BIGINT PK/FK permissions
- granted BOOLEAN NOT NULL; TRUE concede y FALSE deniega, prevaleciendo sobre roles.

## Catálogo

### categories
- id BIGINT PK IDENTITY
- name VARCHAR(100) NOT NULL UNIQUE
- description VARCHAR(255)
- is_active BOOLEAN NOT NULL DEFAULT TRUE

### products
- id BIGINT PK IDENTITY
- name VARCHAR(150) NOT NULL
- description VARCHAR(500)
- price NUMERIC(12,2) NOT NULL CHECK >= 0
- stock INTEGER NOT NULL DEFAULT 0 CHECK >= 0
- minimum_stock INTEGER NOT NULL DEFAULT 0 CHECK >= 0
- category_id BIGINT NOT NULL FK categories
- is_active BOOLEAN NOT NULL DEFAULT TRUE

## Clientes y ventas

### customers
- id BIGINT PK IDENTITY
- name VARCHAR(150) NOT NULL
- document VARCHAR(40) NULL UNIQUE
- phone VARCHAR(40)
- address VARCHAR(255)
- is_active BOOLEAN NOT NULL DEFAULT TRUE

### payment_methods
- id BIGINT PK IDENTITY
- name VARCHAR(60) NOT NULL UNIQUE
- is_active BOOLEAN NOT NULL DEFAULT TRUE

### sales
- id BIGINT PK IDENTITY
- customer_id BIGINT NULL FK customers
- user_id BIGINT NOT NULL FK users
- payment_method_id BIGINT NOT NULL FK payment_methods
- sale_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
- status VARCHAR(20) NOT NULL: DRAFT, COMPLETED o CANCELLED

### sale_details
- id BIGINT PK IDENTITY
- sale_id BIGINT NOT NULL FK sales
- product_id BIGINT NOT NULL FK products
- product_name VARCHAR(150) NOT NULL (instantánea)
- category_name VARCHAR(100) NOT NULL (instantánea)
- quantity INTEGER NOT NULL CHECK > 0
- unit_price NUMERIC(12,2) NOT NULL CHECK >= 0
- subtotal NUMERIC(14,2) GENERATED STORED = quantity * unit_price

## Inventario

### inventory_movements
- id BIGINT PK IDENTITY
- product_id BIGINT NOT NULL FK products
- user_id BIGINT NOT NULL FK users
- sale_detail_id BIGINT NULL FK sale_details
- movement_type VARCHAR(20) NOT NULL: ENTRY, EXIT, SALE, SALE_REVERSAL o ADJUSTMENT
- quantity INTEGER NOT NULL CHECK > 0
- stock_before INTEGER NOT NULL CHECK >= 0
- stock_after INTEGER NOT NULL CHECK >= 0
- note VARCHAR(255)
- created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP

## Relaciones

- categories 1:N products.
- products 1:N inventory_movements y sale_details.
- users 1:N sales e inventory_movements.
- customers 1:N sales; customer_id puede ser NULL en sales.
- payment_methods 1:N sales.
- sales 1:N sale_details.
- sale_details 1:N inventory_movements.
