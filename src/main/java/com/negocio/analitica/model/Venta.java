package com.negocio.analitica.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Sale domain model aligned with PostgreSQL sales and sale_details.
 * customerId is optional; userId and paymentMethodId identify the operator
 * and the payment method used for the sale.
 */
public class Venta {
    private Long id;
    private LocalDateTime fecha;
    private Long clienteId;
    private Long userId;
    private Long paymentMethodId;
    private String status = "DRAFT";
    private String clienteNombre;
    private String metodoPago;
    private List<DetalleVenta> detalles = new ArrayList<>();

    public Venta() { }

    public Venta(Long id, LocalDateTime fecha, Long clienteId, Long userId,
                 Long paymentMethodId, String status, List<DetalleVenta> detalles) {
        this.id = id;
        this.fecha = fecha;
        this.clienteId = clienteId;
        this.userId = userId;
        this.paymentMethodId = paymentMethodId;
        this.status = status == null ? "DRAFT" : status;
        setDetalles(detalles);
    }

    /**
     * Temporary compatibility constructor for the existing SQLite console flow.
     * Remove after PostgreSQL repositories and user sessions are integrated.
     */
    @Deprecated
    public Venta(Long id, LocalDateTime fecha, Long clienteId, String clienteNombre,
                 String metodoPago, List<DetalleVenta> detalles) {
        this.id = id;
        this.fecha = fecha;
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.metodoPago = metodoPago;
        setDetalles(detalles);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(Long paymentMethodId) { this.paymentMethodId = paymentMethodId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    /** @deprecated Resolve the customer using customerId. */
    @Deprecated public String getClienteNombre() { return clienteNombre; }
    /** @deprecated Resolve the customer using customerId. */
    @Deprecated public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    /** @deprecated Use paymentMethodId. */
    @Deprecated public String getMetodoPago() { return metodoPago; }
    /** @deprecated Use paymentMethodId. */
    @Deprecated public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public List<DetalleVenta> getDetalles() { return List.copyOf(detalles); }
    public void setDetalles(List<DetalleVenta> detalles) {
        this.detalles = detalles == null ? new ArrayList<>() : new ArrayList<>(detalles);
    }
    public double getTotal() {
        return detalles.stream().mapToDouble(DetalleVenta::getSubtotal).sum();
    }
    public int getUnidadesVendidas() {
        return detalles.stream().mapToInt(DetalleVenta::getCantidad).sum();
    }
}
