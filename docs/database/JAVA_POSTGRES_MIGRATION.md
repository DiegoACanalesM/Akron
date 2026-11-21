# Java modules adapted to the PostgreSQL model

Branch: `dev1-postgresql-integration`
Base: `dev1-database`

This branch contains the first domain and repository-contract adaptation plus the local single-instance operating rule. It is not yet a completed PostgreSQL migration.

## Operating rule: one active operator

- Only one local AKRON process may be open for a business installation.
- The employee using that process is the only active operator. To hand over the computer, close the current session/application before the next employee starts.
- This version does not support multiple computers sharing one business database.
- `SingleInstanceGuard` obtains an operating-system file lock at `data/akron.lock`. A second local process is rejected while the first process holds the lock. The OS releases the lock when the process exits or closes its channel.
- This process lock is not user authentication. Individual login, password-hash verification, role checks, and session identity remain required integration work.

## Mapping decisions

- A sale may have no customer: `Venta.clienteId` is nullable.
- A sale is associated with the system user who records it through `userId`.
- A sale references `payment_methods` through `paymentMethodId`; new code must not persist a free-text payment label.
- Sale lifecycle uses `DRAFT`, `COMPLETED`, and `CANCELLED`.
- Customer document is optional. If provided, the database unique constraint protects uniqueness.
- Product price accepts zero, matching the PostgreSQL CHECK constraint `price >= 0`.
- Categories and products expose their `is_active` state. Inactive products cannot be sold, and new products cannot be assigned to inactive categories.
- Inventory history is represented by `InventoryMovement` and `InventoryMovementType`; persistence is append-only through the repository contract.
- Sale totals must be read from `vw_sale_totals`, not stored in `sales`.
- Stock updates, sale details, and inventory movement rows must be committed or rolled back together in one database transaction. The single-operator rule simplifies the normal workflow but does not replace transaction boundaries or database constraints.

## Changes made in this branch

- Extended `Venta` with `userId`, `paymentMethodId`, and sale status.
- Added `Usuario` and `MetodoPago` domain models.
- Added `EstadoVenta` and `InventoryMovementType` enumerations.
- Added `InventoryMovement` domain model.
- Made customer document optional in `ClienteService`.
- Extended `Category` and `Product` with active-state fields; Product also includes the database description field.
- Aligned product price validation with PostgreSQL's non-negative constraint.
- Added repository contracts for users, permissions, payment methods, and inventory movements.
- Added BCrypt password hashing (work factor 12) and PostgreSQL JDBC dependencies.
- Added PostgreSQL connection configuration through AKRON_DB_URL, AKRON_DB_USER, and AKRON_DB_PASSWORD environment variables. No database password is stored in source code.
- Added PostgreSQL user and effective-permission repositories. Individual user permission overrides take precedence over role permissions.
- Added AuthenticationService and immutable AuthenticatedSession, including permission checks and clearing the supplied password char array after login attempts.
- Added a first-run OWNER bootstrap that refuses to create the initial owner when an account already exists.
- Added owner-authorized employee creation and account activation/deactivation.
- Added transactional protection for first-owner creation and for the rule that prevents deactivating the last active OWNER.
- Added owner-controlled per-user permission grants, denials, and reset-to-role-default behavior.
- Added unit tests for login, invalid credentials, inactive users, unknown users, session permissions, owner bootstrap, employee-management authorization, and permission administration.
- Added `SingleInstanceGuard` and wired it into application startup to prevent two local AKRON processes from operating at once.

## Pending integration work

1. Complete PostgreSQL repository implementations for the remaining business modules; the connection provider and user/permission repositories are now present.
2. Connect the authentication flow, first-owner setup, and user-management services to application startup; add a secure login/logout screen or console prompt.
3. Add a user-management interface for listing employees and assigning their individual permissions.
4. Implement payment-method lookup.
5. Update `VentaService` to accept an optional customer and require the authenticated user and a valid payment method.
6. Move sale creation and stock movement into one PostgreSQL transaction; preserve database stock constraints.
7. Update the console flow and analytics queries to use the PostgreSQL schema and views.
8. Add unit and PostgreSQL integration tests, including the single-instance lock, and remove SQLite migration scaffolding only after the replacement is verified.

The old SQLite adapter and deprecated fields/constructor in `Venta` are temporary compatibility scaffolding. This branch is **not yet a completed PostgreSQL integration**; the current application still uses SQLite.
