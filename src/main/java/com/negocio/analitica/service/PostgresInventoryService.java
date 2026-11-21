package com.negocio.analitica.service;

import com.negocio.analitica.model.InventoryMovement;
import com.negocio.analitica.repository.PostgresInventoryOperations;
import com.negocio.analitica.security.AuthenticatedSession;
import java.util.List;

/** Inventory use cases backed by atomic PostgreSQL stock and movement operations. */
public final class PostgresInventoryService {
    private final PostgresInventoryOperations inventory;
    private final AuthenticatedSession session;

    public PostgresInventoryService(PostgresInventoryOperations inventory, AuthenticatedSession session) {
        if (inventory == null || session == null) {
            throw new IllegalArgumentException("Inventory repository and authenticated session are required.");
        }
        this.inventory = inventory;
        this.session = session;
    }

    public InventoryMovement registerEntry(Long productId, int quantity, String note) {
        session.requirePermission("INVENTORY_MANAGE");
        return inventory.registerEntry(productId, session.getUserId(), quantity, note);
    }

    public InventoryMovement registerExit(Long productId, int quantity, String note) {
        session.requirePermission("INVENTORY_MANAGE");
        return inventory.registerExit(productId, session.getUserId(), quantity, note);
    }

    public InventoryMovement adjustStock(Long productId, int newStock, String note) {
        session.requirePermission("INVENTORY_MANAGE");
        return inventory.adjustStock(productId, session.getUserId(), newStock, note);
    }

    public List<InventoryMovement> history(Long productId) {
        session.requirePermission("INVENTORY_VIEW");
        return inventory.findByProductId(productId);
    }
}
