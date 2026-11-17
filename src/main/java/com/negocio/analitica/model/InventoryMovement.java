package com.negocio.analitica.model;

import java.time.LocalDateTime;

/** Immutable-history entry describing one stock change. */
public class InventoryMovement {
    private Long id;
    private Long productId;
    private Long userId;
    private Long saleDetailId;
    private InventoryMovementType movementType;
    private int quantity;
    private int stockBefore;
    private int stockAfter;
    private String note;
    private LocalDateTime createdAt;

    public InventoryMovement() { }

    public InventoryMovement(Long id, Long productId, Long userId, Long saleDetailId,
                             InventoryMovementType movementType, int quantity,
                             int stockBefore, int stockAfter, String note,
                             LocalDateTime createdAt) {
        this.id = id;
        this.productId = productId;
        this.userId = userId;
        this.saleDetailId = saleDetailId;
        this.movementType = movementType;
        this.quantity = quantity;
        this.stockBefore = stockBefore;
        this.stockAfter = stockAfter;
        this.note = note;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSaleDetailId() { return saleDetailId; }
    public void setSaleDetailId(Long saleDetailId) { this.saleDetailId = saleDetailId; }
    public InventoryMovementType getMovementType() { return movementType; }
    public void setMovementType(InventoryMovementType movementType) { this.movementType = movementType; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getStockBefore() { return stockBefore; }
    public void setStockBefore(int stockBefore) { this.stockBefore = stockBefore; }
    public int getStockAfter() { return stockAfter; }
    public void setStockAfter(int stockAfter) { this.stockAfter = stockAfter; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
