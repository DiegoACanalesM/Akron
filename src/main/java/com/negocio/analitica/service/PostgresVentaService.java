package com.negocio.analitica.service;

import com.negocio.analitica.model.Cliente;
import com.negocio.analitica.model.DetalleVenta;
import com.negocio.analitica.model.MetodoPago;
import com.negocio.analitica.model.Product;
import com.negocio.analitica.model.Venta;
import com.negocio.analitica.repository.MetodoPagoRepository;
import com.negocio.analitica.repository.PostgresSaleOperations;
import com.negocio.analitica.security.AuthenticatedSession;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL sale use case. It prepares historical sale snapshots but delegates
 * all stock changes and persistence to PostgresVentaRepository's single transaction.
 */
public final class PostgresVentaService extends VentaService {
    private final ClienteLookup customers;
    private final ProductOperations products;
    private final MetodoPagoRepository paymentMethods;
    private final PostgresSaleOperations sales;
    private final AuthenticatedSession session;

    public PostgresVentaService(ClienteLookup customers, ProductOperations products,
                                MetodoPagoRepository paymentMethods, PostgresSaleOperations sales,
                                AuthenticatedSession session) {
        super(customers, products, sales);
        if (paymentMethods == null || session == null) {
            throw new IllegalArgumentException("Payment methods and authenticated session are required.");
        }
        this.customers = customers;
        this.products = products;
        this.paymentMethods = paymentMethods;
        this.sales = sales;
        this.session = session;
    }

    @Override
    public Venta registrar(Venta request) {
        session.requirePermission("SALES_CREATE");
        if (request == null) throw new IllegalArgumentException("La venta es obligatoria.");
        if (request.getPaymentMethodId() == null) {
            throw new IllegalArgumentException("Debe seleccionar un método de pago.");
        }
        if (request.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("La venta debe contener al menos un producto.");
        }

        MetodoPago paymentMethod = paymentMethods.findById(request.getPaymentMethodId())
                .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
        if (!paymentMethod.isActive()) {
            throw new IllegalArgumentException("El método de pago está inactivo.");
        }

        Long customerId = request.getClienteId();
        if (customerId != null) {
            Cliente customer = customers.buscarPorId(customerId);
            customerId = customer.getId();
        }

        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (DetalleVenta detail : request.getDetalles()) {
            if (detail == null || detail.getProductoId() == null || detail.getCantidad() <= 0) {
                throw new IllegalArgumentException("Cada detalle debe indicar producto y cantidad positiva.");
            }
            quantities.merge(detail.getProductoId(), detail.getCantidad(), Math::addExact);
        }

        List<DetalleVenta> snapshots = quantities.entrySet().stream().map(entry -> {
            Product product = products.findProductById(entry.getKey());
            if (!product.isActive()) {
                throw new IllegalArgumentException("El producto está inactivo: " + product.getName());
            }
            String categoryName = product.getCategory() == null
                    ? "Sin categoría" : product.getCategory().getName();
            return new DetalleVenta(null, product.getId(), product.getName(), categoryName,
                    entry.getValue(), product.getPrice());
        }).toList();

        Venta completedSale = new Venta(null, request.getFecha(), customerId,
                session.getUserId(), paymentMethod.getId(), "COMPLETED", snapshots);
        return sales.save(completedSale);
    }

    /** Cancels a completed sale and reverses its stock movements atomically. */
    public Venta cancelar(Long saleId, String note) {
        session.requirePermission("SALES_CANCEL");
        return sales.cancelCompletedSale(saleId, session.getUserId(), note);
    }
}
