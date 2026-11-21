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
- `PostgresClienteRepository`: create, update, find by ID, find by document, list and delete customers. Blank/optional documents are stored as SQL `NULL`, and the database unique constraint remains the final duplicate guard.
- `PostgresMetodoPagoRepository`: find a payment method by ID and list active methods from the PostgreSQL catalog.
- `PostgresVentaRepository`: persist a completed sale and its detail snapshots, conditionally decrement stock, and append one `SALE` inventory movement per detail in a single transaction. Any failure rolls back all of those changes.
- `PostgresVentaService`: prepares sale snapshots using the authenticated operator, optional customer and active payment method; it does not change stock itself and delegates creation to the atomic repository transaction.
- `PostgresSaleOperations` and `PostgresVentaRepository.cancelCompletedSale`: lock the completed sale, restore stock and append `SALE_REVERSAL` movements before marking it cancelled, all in one transaction.
- `PostgresInventoryRepository`: register entries, exits and absolute stock adjustments, updating `products.stock` and appending the corresponding movement in one transaction. It also supports optimistic stock-before validation for supplied movement snapshots.
- `PostgresInventoryService`: applies `INVENTORY_MANAGE` and `INVENTORY_VIEW` permission checks before inventory operations.
- Sale and manual inventory updates now reject inactive products/categories; sale creation also rejects an inactive customer. These checks are enforced again inside the PostgreSQL transaction, not only by the service.
- All repository adapters use the existing `ConnectionProvider` and PostgreSQL schema.
- These adapters are implemented but are not yet wired into application startup. Keeping the current SQLite services together temporarily avoids splitting a sale between SQLite and PostgreSQL while stock updates are still coupled to `ProductService`.

## Current persistence status

| Module | Database |
|---|---|
| Authentication and users | PostgreSQL |
| Permissions | PostgreSQL |
| Categories in running app | SQLite (temporary) |
| Products in running app | SQLite (temporary) |
| PostgreSQL category/product adapters | Implemented, awaiting coordinated cutover |
| Customers in running app | SQLite (temporary) |
| PostgreSQL customer adapter | Implemented, awaiting coordinated cutover |
| Payment methods in running app | Not yet connected |
| PostgreSQL payment method adapter | Implemented, awaiting coordinated cutover |
| PostgreSQL sale repository and transaction | Implemented, not yet wired or integration-tested |
| PostgreSQL sale service | Creation and cancellation implemented, not yet wired into the console flow |
| PostgreSQL inventory repository/service | Entry, exit, adjustment and history implemented, not yet wired into the console flow |
| Live PostgreSQL integration tests | Added; require a PostgreSQL schema and `AKRON_TEST_DB_*` environment variables |
| CI PostgreSQL service | Added to Java CI; schema is created before integration tests |
| Sales and sale details | SQLite (temporary) |
| Inventory movements | Not yet migrated |
| Analytics | Existing SQLite sales source (temporary) |

## Next migration steps

1. Review the first live PostgreSQL CI run and fix any integration issues it reveals.
2. Wire categories, products, customers, payment methods, sales and inventory to PostgreSQL together, including the console cancellation flow.
3. Migrate analytics to PostgreSQL views and queries.
4. Remove SQLite dependencies and adapters after the replacement is verified.
5. Verify the schema and full application flow on a clean local database.

The application is not yet fully migrated: the running commercial modules still depend on SQLite. Do not treat the mixed authentication/business persistence stage as production-ready.
