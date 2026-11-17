package com.negocio.analitica.repository;

import com.negocio.analitica.model.InventoryMovement;
import java.util.List;

/** Persistence contract for the append-only inventory movement history. */
public interface InventoryMovementRepository {
    InventoryMovement save(InventoryMovement movement);
    List<InventoryMovement> findByProductId(Long productId);
}
