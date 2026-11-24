package com.negocio.analitica.service;

import com.negocio.analitica.model.Category;
import com.negocio.analitica.model.Product;
import com.negocio.analitica.model.Venta;
import com.negocio.analitica.repository.VentaRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnaliticaServiceTest {

    @Test
    void lowStockAnalyticsExcludesInactiveProducts() {
        Category category = new Category(1L, "General", "", true);
        Product activeLowStock = new Product(1L, "Active", 10.0, 2, 5, category);
        Product inactiveLowStock = new Product(2L, "Inactive", 10.0, 1, 5, category);
        inactiveLowStock.setActive(false);

        ProductOperations products = new ProductOperations() {
            @Override public Product findProductById(Long id) { return null; }
            @Override public boolean hasAvailability(Long id, int quantity) { return false; }
            @Override public Product updateProduct(Product product) { return product; }
            @Override public List<Product> findAllProducts() {
                return List.of(activeLowStock, inactiveLowStock);
            }
        };

        VentaRepository sales = new VentaRepository() {
            @Override public Venta save(Venta sale) { return sale; }
            @Override public Optional<Venta> findById(Long id) { return Optional.empty(); }
            @Override public List<Venta> findAll() { return List.of(); }
            @Override public List<Venta> findByPeriod(LocalDate from, LocalDate to) {
                return List.of();
            }
        };

        AnaliticaService analytics = new AnaliticaService(sales, products);

        assertEquals(List.of(activeLowStock), analytics.productosConStockBajo());
    }
}
