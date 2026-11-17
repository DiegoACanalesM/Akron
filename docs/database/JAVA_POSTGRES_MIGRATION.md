# Java modules adapted to the PostgreSQL model

Branch: `dev1-postgresql-integration`
Base: `dev1-database`

This branch contains the first domain and repository-contract adaptation. The
stable database-design branch remains unchanged.

## Mapping decisions

- A sale may have no customer: `Venta.clienteId` is nullable.
- A sale is associated with the system user who records it through `userId`.
- A sale references `payment_methods` through `paymentMethodId`; new code must
  not persist a free-text payment label.
- Sale lifecycle uses `DRAFT`, `COMPLETED`, and `CANCELLED`.
- Customer document is optional. If provided, the database unique constraint
  protects uniqueness.
- Product price accepts zero, matching the PostgreSQL CHECK constraint
  `price >= 0`.
- Categories and products expose their `is_active` state. Inactive products
  cannot be sold, and new products cannot be assigned to inactive categories.
- Inventory history is represented by `InventoryMovement` and
  `InventoryMovementType`; persistence is append-only through the repository
  contract.
- Sale totals must be read from `vw_sale_totals`, not stored in `sales`.
- Stock updates, sale details, and inventory movement rows must be committed
  or rolled back together in one database transaction.

## Changes made in this branch

- Extended `Venta` with `userId`, `paymentMethodId`, and sale status.
- Added `Usuario` and `MetodoPago` domain models.
- Added `EstadoVenta` and `InventoryMovementType` enumerations.
- Added `InventoryMovement` domain model.
- Made customer document optional in `ClienteService`.
- Extended `Category` and `Product` with active-state fields; Product also
  includes the database description field.
- Aligned product price validation with PostgreSQL's non-negative constraint.
- Added repository contracts for users, payment methods, and inventory
  movements.

## Pending integration work

1. Implement PostgreSQL connection configuration and repositories for all
   existing modules.
2. Implement user lookup/session and payment-method lookup.
3. Update `VentaService` to accept an optional customer and require the
   authenticated user and a valid payment method.
4. Move sale creation and stock movement into one PostgreSQL transaction.
5. Update the console flow and analytics queries to use the PostgreSQL schema
   and views.
6. Add integration tests against PostgreSQL and remove SQLite migration
   scaffolding only after the replacement is verified.

The old SQLite adapter and deprecated fields/constructor in `Venta` are
temporary compatibility scaffolding. This branch is **not yet a completed
PostgreSQL integration**; it contains the first adaptation layer only.
