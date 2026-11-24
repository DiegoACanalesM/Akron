# AVNBR — Análisis de Ventas para Negocios de Bajo Recursos

**AKRON** es un sistema Java 17 para pequeños negocios: categorías, productos, inventario, clientes, ventas e indicadores comerciales.

## Ejecución

Requisitos: Java 17, Maven 3.9 o superior y PostgreSQL instalado localmente.

1. Prepara PostgreSQL y ejecuta los scripts en el orden indicado en [database/README.md](database/README.md).
2. Configura las variables de entorno:
   - `AKRON_DB_URL`: opcional; por defecto `jdbc:postgresql://localhost:5432/akron_db`.
   - `AKRON_DB_USER`: usuario PostgreSQL con permisos sobre `akron_db`.
   - `AKRON_DB_PASSWORD`: contraseña de ese usuario.
3. Desde la raíz del repositorio, ejecuta:

```bash
mvn clean verify
mvn javafx:run
```

Para iniciar la versión de consola anterior, todavía disponible durante la transición:

```bash
mvn exec:java -Dexec.mainClass="com.negocio.analitica.Application"
```

En el primer inicio, AKRON solicita crear la cuenta inicial OWNER. En los siguientes inicios solicita usuario y contraseña; tras cinco intentos fallidos, se cierra.

## Persistencia

La rama `dev1-postgresql-integration` conecta autenticación, usuarios, permisos y todos los módulos comerciales a PostgreSQL. Categorías, productos, clientes, métodos de pago, ventas, detalles e inventario comparten la misma base local.

La creación y cancelación de ventas actualizan existencias y registran movimientos de inventario en transacciones atómicas. Las entradas, salidas y ajustes manuales también guardan el historial junto con la actualización del stock. Los cambios de existencias no deben hacerse directamente desde una edición de producto. Los productos activos de categorías desactivadas no pueden venderse, pero sus existencias siguen disponibles para consulta y gestión de inventario.

No se migran registros antiguos de SQLite: se utiliza una base PostgreSQL nueva. Los adaptadores SQLite siguen en el repositorio por compatibilidad y pruebas existentes, pero ya no se conectan desde `Application`.

## Uso exclusivo local

Solo se permite una instancia de AKRON abierta por instalación. Para cambiar de empleado, se debe cerrar la aplicación actual antes de que ingrese el siguiente. Esta versión no permite que varios computadores compartan la misma base de datos.

## Funciones disponibles

1. Registrar categorías.
2. Registrar productos asociados a categorías.
3. Registrar clientes.
4. Registrar ventas con uno o varios productos y métodos de pago del catálogo.
5. Consultar categorías, productos, clientes y ventas, incluyendo el detalle de cada venta.
6. Editar y desactivar categorías, productos y clientes sin borrar su historial comercial.
7. Consultar ingresos, transacciones, unidades, ticket promedio, producto más vendido, ventas por categoría y método de pago, y alertas de stock bajo.
8. Comparar los resultados de ventas entre períodos equivalentes.
9. Importar clientes desde un archivo CSV.
10. Registrar entradas, salidas, ajustes y consultar el historial de inventario.
11. Administrar usuarios y permisos.
12. Cancelar ventas completadas, restaurando existencias y dejando trazabilidad.

## Formato CSV de clientes

El archivo debe estar codificado en UTF-8, con encabezado y cuatro columnas en este orden:

```csv
nombre,documento,telefono,direccion
Tienda Central,100200300,3001234567,Calle 10
Minimercado La 12,100200301,3007654321,Carrera 12
```

Los documentos duplicados o las filas inválidas se rechazan sin detener toda la importación. Para esta versión, los campos no deben contener comas internas.

## Arquitectura

`Consola → Controller → Service → Repository → PostgreSQL`

- **Model:** entidades del dominio.
- **Controller:** coordinación de operaciones.
- **Service:** validaciones y reglas de negocio.
- **Repository:** contratos y adaptadores de persistencia.
- **PostgreSQL/JDBC:** almacenamiento local persistente.

## Tecnologías

- Java 17
- Maven
- PostgreSQL JDBC
- SQLite JDBC (adaptadores heredados)
- JUnit 5
- GitHub Actions

## Verificación

Ejecuta `mvn clean verify` para compilar y ejecutar las pruebas automatizadas. Java CI crea una instancia PostgreSQL temporal, aplica el esquema y ejecuta pruebas de integración de ventas, cancelaciones e inventario.

## Alcance y estado

La interfaz JavaFX de escritorio está en construcción en la rama `feature/javafx-desktop-ui`. Esta primera etapa incorpora inicio de sesión y configuración inicial del propietario, ventana principal, dashboard con indicadores reales, consulta de productos y categorías e identificación de productos con stock bajo. Las pantallas de inventario y reportes están en su primera versión de consulta; todavía faltan formularios de edición, movimientos desde la interfaz, ventas, gráficos y exportación.

La lógica de negocio y la persistencia siguen separadas de la interfaz. La aplicación móvil no está incluida en esta entrega: una futura versión móvil deberá comunicarse con un servicio/API seguro en lugar de conectarse directamente a PostgreSQL. La interfaz JavaFX debe probarse en un equipo local con PostgreSQL antes de considerarse lista para uso real. Consulta [la guía de migración](docs/database/JAVA_POSTGRES_MIGRATION.md) para los detalles técnicos.
