package com.negocio.analitica.controller;

import com.negocio.analitica.model.InventoryMovement;
import com.negocio.analitica.service.PostgresInventoryService;
import java.util.List;

/** Application boundary for manual inventory operations and movement history. */
public final class InventoryController {
    private final PostgresInventoryService service;

    public InventoryController(PostgresInventoryService service) {
        if (service == null) throw new IllegalArgumentException("PostgresInventoryService cannot be null.");
        this.service = service;
    }

    public InventoryMovement registerEntry(Long productId, int quantity, String note) {
        return service.registerEntry(productId, quantity, note);
    }

    public InventoryMovement registerExit(Long productId, int quantity, String note) {
        return service.registerExit(productId, quantity, note);
    }

    public InventoryMovement adjustStock(Long productId, int newStock, String note) {
        return service.adjustStock(productId, newStock, note);
    }

    public List<InventoryMovement> getHistory(Long productId) {
        return service.history(productId);
    }
}
