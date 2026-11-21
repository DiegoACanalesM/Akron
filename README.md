# AVNBR — Análisis de Ventas para Negocios de Bajo Recursos

Sistema Java 17 para pequeños negocios: categorías, productos, inventario, clientes, ventas e indicadores comerciales.

## Ejecución

Requisitos: Java 17 y Maven 3.9 o superior.

Desde la raíz del repositorio:

```bash
mvn clean verify
mvn exec:java -Dexec.mainClass="com.negocio.analitica.Application"
```

Antes de ejecutar AKRON, configura la conexión local a PostgreSQL mediante estas variables de entorno:

- `AKRON_DB_URL`: opcional; por defecto `jdbc:postgresql://localhost:5432/akron_db`.
- `AKRON_DB_USER`: usuario PostgreSQL con permisos sobre `akron_db`.
- `AKRON_DB_PASSWORD`: contraseña de ese usuario.

La base y los scripts PostgreSQL deben estar preparados según `database/README.md`. En el primer inicio, AKRON solicita crear la cuenta inicial OWNER. En los siguientes inicios solicita usuario y contraseña; tras cinco intentos fallidos, se cierra.

La autenticación y los permisos ya se consultan en PostgreSQL. **Los módulos comerciales (categorías, productos, clientes y ventas) todavía utilizan SQLite** en esta rama y su migración a PostgreSQL está pendiente. No se deben confundir las dos persistencias.

**Uso exclusivo local:** solo se permite una instancia de AKRON abierta por instalación. Para cambiar de empleado, se debe cerrar la aplicación actual antes de que ingrese el siguiente.

## Funciones disponibles en el menú

1. Registrar categorías.
2. Registrar productos asociados a categorías.
3. Registrar clientes.
4. Registrar ventas con uno o varios productos; se valida el stock y se descuenta al confirmar.
5. Consultar categorías, productos, clientes y ventas almacenados.
6. Consultar ingresos, transacciones, unidades, ticket promedio, producto más vendido y alertas de stock bajo.
7. Importar clientes desde un archivo CSV externo.

### Formato CSV de clientes

El archivo debe estar codificado en UTF-8, con encabezado y cuatro columnas en este orden:

```csv
nombre,documento,telefono,direccion
Tienda Central,100200300,3001234567,Calle 10
Minimercado La 12,100200301,3007654321,Carrera 12
```

Los documentos duplicados o las filas inválidas se rechazan sin detener toda la importación. Para esta versión, los campos no deben contener comas internas. Al elegir la opción 7, ingrese la ruta local del archivo.

## Arquitectura actual

- Autenticación y usuarios: `Consola → AuthenticationService / UserManagementService → PostgreSQL`.
- Módulos comerciales pendientes de migración: `Consola → Controller → Service → Repository → SQLite`.

- **Model:** entidades del dominio.
- **Controller:** coordinación de operaciones.
- **Service:** validaciones y reglas de negocio.
- **Repository:** contratos de persistencia.
- **SQLite/JDBC:** almacenamiento local persistente.

## Tecnologías

- Java 17
- Maven
- SQLite JDBC
- JUnit 5
- GitHub Actions

## Verificación

Ejecute `mvn clean verify` para compilar y ejecutar las pruebas automatizadas.

## PostgreSQL y autenticación

Los scripts de `database/` crean el esquema `akron_db`, roles, permisos y métodos de pago. La aplicación utiliza PostgreSQL para autenticación, gestión de usuarios y permisos. Consulta `docs/database/JAVA_POSTGRES_MIGRATION.md` para conocer lo que ya está integrado y lo que sigue pendiente.

## Alcance

La aplicación sigue siendo una interfaz de consola; no es todavía una aplicación web ni una API REST. La rama es una etapa de migración: autenticación y usuarios usan PostgreSQL, mientras los datos comerciales siguen en SQLite. Cada instalación admite un solo operador activo a la vez.
