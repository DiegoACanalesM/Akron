# Java modules adapted to the PostgreSQL model

Branch: `dev1-postgresql-integration`
Base: `dev1-database`

AKRON is being migrated incrementally to PostgreSQL. The target is a fresh PostgreSQL database; existing SQLite business records do not need to be transferred.

## Operating rule: one active operator

- Only one local AKRON process may be open for a business installation.
- The employee using that process is the only active operator. To hand over the computer, close the current session/application before the next employee starts.
- This version does not support multiple computers sharing one business database.
- `SingleInstanceGuard` obtains an operating-system file lock at `data/akron.lock`. A second local process is rejected while the first process holds the lock.
- Authentication is backed by PostgreSQL. The process lock does not replace password verification or permission checks.

## Mapping decisions

- A sale may have no customer: `Venta.clienteId` is nullable.
- A sale is associated with the system user who records it through `userId`.
- A sale references `payment_methods` through `paymentMethodId`.
- Sale lifecycle uses `DRAFT`, `COMPLETED`, and `CANCELLED`.
- Customer document is optional.
- Product price accepts zero, matching PostgreSQL's `price >= 0` constraint.
- Categories and products expose their `is_active` state.
- Inventory history is append-only through `InventoryMovement` and `InventoryMovementType`.
- Sale totals must be read from `vw_sale_totals`.
- Stock updates, sale details, and inventory movement rows must commit or roll back together in one PostgreSQL transaction.

## PostgreSQL repository work completed

- `PostgresCategoryRepository`: create, update, find, list and delete categories, including active state.
- `PostgresProductRepository`: create, update, find, list and delete products, including description, active state, category relation and numeric price.
- Both adapters use the existing `ConnectionProvider` and PostgreSQL schema.
- These adapters are implemented but are not yet wired into application startup. Keeping the current SQLite services together temporarily avoids splitting a sale between SQLite and PostgreSQL while stock updates are still coupled to `ProductService`.

## Current persistence status

| Module | Database |
|---|---|
| Authentication and users | PostgreSQL |
| Permissions | PostgreSQL |
| Categories in running app | SQLite (temporary) |
| Products in running app | SQLite (temporary) |
| PostgreSQL category/product adapters | Implemented, awaiting coordinated cutover |
| Customers | SQLite (temporary) |
| Sales and sale details | SQLite (temporary) |
| Inventory movements | Not yet migrated |
| Analytics | Existing SQLite sales source (temporary) |

## Next migration steps

1. Implement PostgreSQL customer repository.
2. Implement payment-method lookup and PostgreSQL sale persistence.
3. Implement sale creation/cancellation and stock movements in one transaction.
4. Wire categories, products, customers, sales and inventory to PostgreSQL together.
5. Migrate analytics to PostgreSQL views and queries.
6. Remove SQLite dependencies and adapters after the replacement is verified.
7. Add PostgreSQL integration tests and verify the schema on a clean local database.

The application is not yet fully migrated: the running commercial modules still depend on SQLite. Do not treat the mixed authentication/business persistence stage as production-ready.
