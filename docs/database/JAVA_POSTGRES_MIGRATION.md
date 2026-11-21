# Java modules adapted to PostgreSQL

Branch: `dev1-postgresql-integration`

AKRON now uses a fresh local PostgreSQL database for authentication and all commercial modules. Existing SQLite business records are not migrated; the application starts with a new PostgreSQL schema.

## Operating model

- One local AKRON process may be open for a business installation.
- The employee using that process is the only active operator. Close the application before another employee signs in.
- This version does not support multiple computers sharing one business database.
- `SingleInstanceGuard` obtains an operating-system file lock at `data/akron.lock`.
- Authentication and effective permissions are loaded from PostgreSQL.

## Domain and persistence decisions

- A sale may have no customer in the database model; the current console flow asks the operator to select one.
- A sale is associated with the operator through `user_id` and with a payment method through `payment_method_id`.
- Sale lifecycle uses `DRAFT`, `COMPLETED`, and `CANCELLED`.
- Customer document is optional.
- Product price accepts zero.
- Categories and products expose their active state. Sales and manual inventory movements reject inactive products or categories.
- Inventory history is append-only through `InventoryMovement` and `InventoryMovementType`.
- Sale totals are derived from saved detail snapshots; PostgreSQL also provides `vw_sale_totals`.
- Stock updates, sale details and inventory movement rows commit or roll back together in one PostgreSQL transaction.
- Manual stock entries, exits and adjustments also update `products.stock` and append the movement in one transaction.

## PostgreSQL repositories and services

- `PostgresCategoryRepository`: category CRUD and active state.
- `PostgresProductRepository`: product CRUD, category relation, description, price, stock and active state.
- `PostgresClienteRepository`: customer CRUD and optional document lookup.
- `PostgresMetodoPagoRepository`: active payment method catalog.
- `PostgresVentaRepository`: sale persistence, detail snapshots, conditional stock deduction and `SALE` movement creation in one transaction.
- `PostgresVentaRepository.cancelCompletedSale`: locks a completed sale, restores stock, appends `SALE_REVERSAL` movements and marks the sale cancelled in one transaction.
- `PostgresInventoryRepository`: atomic stock entries, exits, absolute adjustments and movement history.
- `PostgresVentaService`: validates the active customer/payment method, captures product and category snapshots, checks permissions and delegates stock changes to the atomic repository.
- `PostgresInventoryService`: enforces `INVENTORY_MANAGE` and `INVENTORY_VIEW`.
- `ProductService`: uses PostgreSQL inventory operations for stock changes. New products with opening stock are created at zero and then receive a recorded initial-stock movement. In PostgreSQL mode, a regular product update cannot silently change stock.
- `Application`: wires all commercial modules to PostgreSQL; it no longer starts SQLite repositories.
- `ConsoleMenu`: selects a payment method from the PostgreSQL catalog and exposes cancellation for completed sales.

## Current persistence status

| Module | Running application |
|---|---|
| Authentication and users | PostgreSQL |
| Permissions | PostgreSQL |
| Categories | PostgreSQL |
| Products | PostgreSQL |
| Customers | PostgreSQL |
| Payment methods | PostgreSQL |
| Sales and sale details | PostgreSQL |
| Inventory and movement history | PostgreSQL |
| Analytics | PostgreSQL sales repository; calculations currently performed in the service layer |
| SQLite adapters | Retained in source for legacy compatibility/tests; not wired by `Application` |

## Integration verification

Java CI starts a temporary PostgreSQL 16 service, applies `database/02_create_tables.sql` through `database/05_initial_data.sql`, and runs the live integration suite.

The initial PostgreSQL repository suite passed with 36 total tests, including 5 live PostgreSQL integration tests, 0 failures, 0 errors and 0 skipped. It covers:
- Atomic sale creation and cancellation with stock restoration and movement history.
- Rollback of a sale when stock is insufficient.
- Inventory entry, exit and absolute adjustment.
- Rejection of inactive customers for sales.
- Customer persistence with an optional document.

The current full-application wiring is also being verified by Java CI and Maven verification. A passing CI run does not replace a local end-to-end test on the user's PC.

## Remaining work

1. Verify the latest full-application wiring with Java CI and PostgreSQL integration tests.
2. Test on a clean local PostgreSQL database, including first OWNER creation, employee permissions, product creation, opening stock, sales, cancellation, and inventory history.
3. Move analytics calculations to PostgreSQL views/queries where that improves correctness and efficiency.
4. Decide when to remove the legacy SQLite adapters and dependency after compatibility is confirmed.
5. Add user-facing access to inventory adjustments/history if required by the final menu design.
6. Prepare local backup and restore instructions before using real business data.

Do not merge this branch into `main` until local end-to-end verification and review are complete.
