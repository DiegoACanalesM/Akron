# AKRON - Diagrama entidad-relación

~~~mermaid
erDiagram
    ROLES ||--o{ USER_ROLES : assigned
    USERS ||--o{ USER_ROLES : has
    ROLES ||--o{ ROLE_PERMISSIONS : grants
    PERMISSIONS ||--o{ ROLE_PERMISSIONS : included
    USERS ||--o{ USER_PERMISSIONS : overrides
    PERMISSIONS ||--o{ USER_PERMISSIONS : configured
    CATEGORIES ||--o{ PRODUCTS : classifies
    PRODUCTS ||--o{ INVENTORY_MOVEMENTS : tracks
    USERS ||--o{ INVENTORY_MOVEMENTS : registers
    CUSTOMERS o|--o{ SALES : associated
    USERS ||--o{ SALES : creates
    PAYMENT_METHODS ||--o{ SALES : paid_with
    SALES ||--|{ SALE_DETAILS : contains
    PRODUCTS ||--o{ SALE_DETAILS : sold_as
    SALE_DETAILS ||--o{ INVENTORY_MOVEMENTS : causes
~~~

Una venta puede tener cero o un cliente asociado. Cada venta confirmada debe tener al menos un detalle; esta regla se valida en la transacción de la aplicación.
