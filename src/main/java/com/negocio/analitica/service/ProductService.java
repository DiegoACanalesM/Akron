package com.negocio.analitica.service;

import com.negocio.analitica.model.Category;
import com.negocio.analitica.model.Inventory;
import com.negocio.analitica.model.Product;
import com.negocio.analitica.repository.CategoryRepository;
import com.negocio.analitica.repository.ProductRepository;
import java.util.List;

public class ProductService implements ProductOperations {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final Inventory inventory = new Inventory();

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository) {
        if (productRepository == null || categoryRepository == null) {
            throw new IllegalArgumentException("Repositories cannot be null.");
        }
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public Product createProduct(Product product) {
        validateProduct(product);
        product.setCategory(findExistingCategory(product.getCategory()));
        return productRepository.save(product);
    }

    public Product updateProduct(Product product) {
        validateProduct(product);
        if (product.getId() == null || productRepository.findById(product.getId()).isEmpty()) {
            throw new IllegalArgumentException("Product does not exist.");
        }
        product.setCategory(findExistingCategory(product.getCategory()));
        return productRepository.save(product);
    }

    @Override
    public Product findProductById(Long id) {
        if (id == null) throw new IllegalArgumentException("Product ID cannot be null.");
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));
    }

    @Override
    public List<Product> findAllProducts() { return productRepository.findAll(); }

    public void deleteProduct(Long id) {
        findProductById(id);
        productRepository.deleteById(id);
    }

    @Override
    public boolean hasAvailability(Long productId, int quantity) {
        Product product = findProductById(productId);
        return product.isActive() && inventory.hasAvailability(product.getStock(), quantity);
    }

    public Product registerStockEntry(Long productId, int quantity) {
        Product product = findProductById(productId);
        inventory.registerEntry(product, quantity);
        return productRepository.save(product);
    }

    public Product registerStockExit(Long productId, int quantity) {
        Product product = findProductById(productId);
        inventory.registerExit(product, quantity);
        return productRepository.save(product);
    }

    public boolean hasLowStock(Long productId) {
        return inventory.hasLowStock(findProductById(productId));
    }

    public List<Product> findLowStockProducts() {
        return productRepository.findAll().stream()
                .filter(Product::isActive)
                .filter(inventory::hasLowStock)
                .toList();
    }

    private Category findExistingCategory(Category category) {
        if (category == null || category.getId() == null) {
            throw new IllegalArgumentException("A valid category is required.");
        }
        Category existing = categoryRepository.findById(category.getId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));
        if (!existing.isActive()) {
            throw new IllegalArgumentException("Cannot assign a product to an inactive category.");
        }
        return existing;
    }

    private void validateProduct(Product product) {
        if (product == null) throw new IllegalArgumentException("Product cannot be null.");
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty.");
        }
        // PostgreSQL products.price accepts zero (NUMERIC CHECK price >= 0).
        if (product.getPrice() < 0 || Double.isNaN(product.getPrice())
                || Double.isInfinite(product.getPrice())) {
            throw new IllegalArgumentException("Price must be a non-negative finite value.");
        }
        if (product.getStock() < 0) throw new IllegalArgumentException("Stock cannot be negative.");
        if (product.getMinimumStock() < 0) {
            throw new IllegalArgumentException("Minimum stock cannot be negative.");
        }
    }
}
