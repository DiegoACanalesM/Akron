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
mvn exec:java -Dexec.mainClass="com.negocio.analitica.Application"
```

En el primer inicio, AKRON solicita crear la cuenta inicial OWNER. En los siguientes inicios solicita usuario y contraseña; tras cinco intentos fallidos, se cierra.

## Persistencia

La rama `dev1-postgresql-integration` conecta autenticación, usuarios, permisos y todos los módulos comerciales a PostgreSQL. Categorías, productos, clientes, métodos de pago, ventas, detalles e inventario comparten la misma base local.

La creación y cancelación de ventas actualizan existencias y registran movimientos de inventario en transacciones atómicas. Las entradas, salidas y ajustes manuales también guardan el historial junto con la actualización del stock. Los cambios de existencias no deben hacerse directamente desde una edición de producto.

No se migran registros antiguos de SQLite: se utiliza una base PostgreSQL nueva. Los adaptadores SQLite siguen en el repositorio por compatibilidad y pruebas existentes, pero ya no se conectan desde `Application`.

## Uso exclusivo local

Solo se permite una instancia de AKRON abierta por instalación. Para cambiar de empleado, se debe cerrar la aplicación actual antes de que ingrese el siguiente. Esta versión no permite que varios computadores compartan la misma base de datos.

## Funciones disponibles

1. Registrar categorías.
2. Registrar productos asociados a categorías.
3. Registrar clientes.
4. Registrar ventas con uno o varios productos y métodos de pago del catálogo.
5. Consultar categorías, productos, clientes y ventas.
6. Consultar ingresos, transacciones, unidades, ticket promedio, producto más vendido y alertas de stock bajo.
7. Importar clientes desde un archivo CSV.
8. Registrar entradas y salidas de inventario.
9. Administrar usuarios y permisos.
10. Cancelar ventas completadas, restaurando existencias y dejando trazabilidad.

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

La aplicación sigue siendo una interfaz de consola; no es todavía una aplicación web ni una API REST. La integración PostgreSQL ya está conectada en la rama, pero antes de considerarla lista para uso real debe probarse también en el PC local y verificarse el flujo completo con una base nueva. Consulta [la guía de migración](docs/database/JAVA_POSTGRES_MIGRATION.md) para los detalles técnicos.
