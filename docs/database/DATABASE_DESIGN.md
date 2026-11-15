# AKRON - Diseño de base de datos (versión preliminar 1.0)

## Alcance y despliegue

Cada negocio tendrá una instalación local e independiente de AKRON y PostgreSQL en un computador. No se contempla sincronización entre negocios en esta versión. La aplicación Java se conectará mediante JDBC a localhost:5432, base akron_db.

Flujo obligatorio: UI -> Controller -> Service -> Repository -> PostgreSQL. Las credenciales deben estar fuera del código fuente.

## Entidades

### Usuarios y permisos
- roles: perfiles OWNER y EMPLOYEE.
- permissions: catálogo de permisos.
- users: cuentas de acceso.
- user_roles: roles asignados a usuarios.
- role_permissions: permisos predeterminados por rol.
- user_permissions: excepciones individuales que conceden o deniegan permisos.

La excepción individual prevalece sobre los permisos heredados. Si un usuario tiene varios roles, basta con que uno conceda el permiso, salvo que exista una excepción individual. La aplicación debe proteger las operaciones administrativas y evitar desactivar o eliminar al último OWNER. Las contraseñas se almacenan únicamente como hashes seguros (Argon2id o bcrypt), nunca en texto plano.

### Catálogo e inventario
- categories: clasificación de productos.
- products: catálogo, precio y existencia actual.
- inventory_movements: historial de entradas, salidas, ventas, reversiones y ajustes.

products.stock representa las existencias actuales. Cada cambio debe actualizar stock y registrar el movimiento en una misma transacción. Los movimientos históricos no se editan ni eliminan; los errores se corrigen mediante movimientos compensatorios.

### Clientes y ventas
- customers: clientes registrados opcionales.
- payment_methods: medios de pago.
- sales: cabecera de venta y usuario responsable.
- sale_details: productos vendidos y datos históricos.

sales.customer_id admite NULL. Cada venta confirmada debe tener al menos un detalle. El detalle conserva el nombre del producto, categoría y precio al momento de la venta.

## Relaciones

- categories 1:N products.
- products 1:N inventory_movements.
- users 1:N sales e inventory_movements.
- customers 1:N sales; el cliente de una venta es opcional.
- payment_methods 1:N sales.
- sales 1:N sale_details.
- products 1:N sale_details.
- sale_details 1:N inventory_movements para venta y eventual reversión.

## Normalización

El diseño aplica 1FN, 2FN y 3FN:
- 1FN: valores atómicos; permisos y detalles repetibles en tablas separadas.
- 2FN: tablas puente con clave compuesta y sin atributos dependientes de una parte de la clave.
- 3FN: los datos descriptivos se almacenan en su entidad y se referencian con claves foráneas.

Las columnas sale_details.product_name y category_name son instantáneas históricas intencionales; unit_price conserva el precio cobrado. No sustituyen la relación con products. El subtotal se calcula con una columna generada; el total de venta se calcula mediante una vista.

## Reglas de inventario

Tipos:
- ENTRY: entrada manual.
- EXIT: salida manual.
- SALE: descuento originado por una venta.
- SALE_REVERSAL: devolución de stock al revertir una venta.
- ADJUSTMENT: ajuste manual positivo o negativo.

quantity siempre es positiva. stock_before y stock_after permiten auditar el resultado. Para ADJUSTMENT, la diferencia absoluta debe coincidir con quantity. La aplicación debe impedir stock negativo bajo concurrencia y ejecutar el cambio de stock, el movimiento y la venta en una única transacción JDBC.

## Analítica

Los indicadores se calculan mediante consultas y vistas SQL. No se duplican métricas que puedan derivarse de ventas y detalles.

## Instalación

1. Ejecutar database/01_create_database.sql conectado a una base administrativa como postgres.
2. Conectarse a akron_db.
3. Ejecutar en orden 02_create_tables.sql, 03_create_indexes.sql, 04_create_views.sql y 05_initial_data.sql.
4. Crear el primer usuario OWNER mediante un flujo de configuración inicial seguro. No incluir contraseñas reales en scripts.

## Coordinación

Este es un diseño preliminar en dev1-database. Antes de integrarlo, revisar entidades y contratos con el líder y Dev 2; confirmar estados y reglas de ventas; alinear repositorios Java y transacciones; añadir pruebas automatizadas y procedimientos de respaldo/restauración.
