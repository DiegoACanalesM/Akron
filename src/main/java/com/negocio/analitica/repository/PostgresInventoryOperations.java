package com.negocio.analitica.repository;

import com.negocio.analitica.model.InventoryMovement;

/** Inventory changes that must update stock and append history atomically. */
public interface PostgresInventoryOperations extends InventoryMovementRepository {
    InventoryMovement registerEntry(Long productId, Long userId, int quantity, String note);
    InventoryMovement registerExit(Long productId, Long userId, int quantity, String note);
    InventoryMovement adjustStock(Long productId, Long userId, int newStock, String note);
}
