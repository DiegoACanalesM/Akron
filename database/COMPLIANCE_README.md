# AKRON - Cumplimiento del verificador académico

Esta carpeta contiene una preparación SQL separada para los 27 criterios del
archivo oficial \`verificar_proyecto.sql\`. No modifica código Java.

## Estado y precauciones

- Los scripts originales \`01_create_database.sql\` a \`05_initial_data.sql\`
  se conservan sin cambios.
- Los scripts de cumplimiento no se ejecutan automáticamente durante la
  instalación normal.
- No ejecutar una migración sobre una base con datos reales sin respaldo y
  revisión previa.
- \`09_validation_seed.sql\` es exclusivamente para una base de evaluación
  desechable y utiliza datos ficticios.
- El validador oficial debe conservarse intacto.

## Orden para una base de evaluación

1. Crear la base y aplicar los scripts originales 01 a 05.
2. Crear el primer usuario activo de AKRON mediante el flujo seguro de la
   aplicación.
3. Ejecutar \`06_compliance_schemas.sql\`.
4. Ejecutar \`07_compliance_reports_audit.sql\`.
5. Ejecutar \`08_compliance_security_indexes.sql\`.
6. Establecer una contraseña segura para \`akron_app\` fuera del repositorio.
7. Configurar el entorno Java con \`AKRON_DB_USER=akron_app\` y
   \`AKRON_DB_PASSWORD\` sin guardar la contraseña en archivos versionados.
8. Ejecutar \`09_validation_seed.sql\` únicamente en la base desechable de
   evaluación.
9. Ejecutar el verificador oficial y guardar la salida en evidencias.

## Compatibilidad Java

Las tablas conservan sus nombres. La base establece \`search_path = app, public\`
para que consultas Java no calificadas como \`SELECT ... FROM products\`
resuelvan el objeto en el esquema \`app\`.

El cambio de \`search_path\` se aplica a conexiones nuevas. Reiniciar la
aplicación después de la migración.

## Seguridad

El rol \`akron_app\` se crea sin contraseña. Establecerla por un canal local
seguro, por ejemplo con el comando interactivo \`\\password akron_app\` en psql.
No incluir contraseñas reales en SQL versionado.

Los tres roles de grupo son:
- \`akron_readonly\`: lectura.
- \`akron_operator\`: operaciones CRUD de la aplicación.
- \`akron_admin\`: administración de datos.

## Limitaciones conocidas que deben probarse

- Los triggers de auditoría registran el usuario de PostgreSQL, no el usuario
  humano autenticado dentro de AKRON.
- El trigger de inventario comprueba que \`stock_after\` coincida con el stock
  actual del producto. Debe verificarse con las pruebas de ventas, reversión y
  ajustes antes de utilizarlo en una instalación real.
- La semilla académica requiere un usuario activo y catálogos base creados.
- El verificador comprueba cantidades y estructura; la calidad funcional se
  debe demostrar con las pruebas y el informe del proyecto.
