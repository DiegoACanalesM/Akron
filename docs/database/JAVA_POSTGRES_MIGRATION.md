# Java modules adapted to the PostgreSQL model

Branch: `dev1-postgresql-integration`

This branch starts the Java-side adaptation without changing the stable
`dev1-database` branch.

## Mapping decisions

- A sale may have no customer: `Venta.clienteId` is nullable.
- Every sale must identify the logged-in system user through `userId`.
- Every sale must reference a payment method catalog entry through
  `paymentMethodId`, not a free-text payment label.
- Sale lifecycle is represented by `DRAFT`, `COMPLETED`, and `CANCELLED`.
- Customer document is optional. If supplied, PostgreSQL's unique constraint
  protects uniqueness; PostgreSQL permits multiple NULL values in a UNIQUE
  column.
- Sale totals are derived from sale details and will be read from
  `vw_sale_totals`; they are not persisted in `sales`.
- Inventory changes must create `inventory_movements` records and update
  product stock within one database transaction.

## Work staged

1. Extend the sale domain model with user, payment-method, and status fields.
2. Make customer document optional in the service layer.
3. Add domain types for users and payment methods.
4. Next: add user/payment-method repositories, PostgreSQL adapters, and a
   transaction boundary shared by sale, stock, and movement persistence.
5. Finally: update the console flow to require a signed-in user and select a
   payment method, while allowing a sale without a customer.

The old SQLite adapter and compatibility sale constructor are temporary
migration scaffolding. They must be removed after the PostgreSQL adapter is
active. This branch is not yet a completed PostgreSQL integration.
