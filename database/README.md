# Scripts PostgreSQL de AKRON

Ejecutar en este orden:

1. 01_create_database.sql conectado a la base administrativa postgres.
2. Conectarse a akron_db.
3. 02_create_tables.sql.
4. 03_create_indexes.sql.
5. 04_create_views.sql.
6. 05_initial_data.sql.

## Desde pgAdmin

Abrir Query Tool conectado a postgres y ejecutar el primer script. Luego cambiar la conexión a akron_db y ejecutar los scripts restantes, uno por uno y en orden.

## Desde psql

~~~bash
psql -U postgres -d postgres -f database/01_create_database.sql
psql -U postgres -d akron_db -f database/02_create_tables.sql
psql -U postgres -d akron_db -f database/03_create_indexes.sql
psql -U postgres -d akron_db -f database/04_create_views.sql
psql -U postgres -d akron_db -f database/05_initial_data.sql
~~~

El primer script requiere privilegio CREATEDB. No se ejecuta conectado a akron_db.

## Notas

- Son scripts para una base nueva; no migran datos de SQLite.
- No se versionan credenciales ni contraseñas.
- El primer usuario OWNER debe crearse mediante un flujo de configuración inicial seguro.
- Probar en desarrollo antes de usar datos reales.
- Preparar respaldos periódicos y probar la restauración.


## Preparación para el verificador académico

Los scripts `06_compliance_schemas.sql` a `09_validation_seed.sql` son
opcionales y están separados de la instalación normal para no cambiar el flujo
actual de la aplicación sin revisión.

Consulta [COMPLIANCE_README.md](COMPLIANCE_README.md) antes de utilizarlos.
La migración de esquemas requiere respaldo previo. La semilla contiene datos
ficticios y solo debe ejecutarse en una base desechable de evaluación.
