package com.negocio.analitica.model;

/** Movement types accepted by the PostgreSQL inventory_movements constraint. */
public enum InventoryMovementType {
    ENTRY,
    EXIT,
    SALE,
    SALE_REVERSAL,
    ADJUSTMENT
}
