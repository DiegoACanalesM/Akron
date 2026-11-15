-- Ejecutar después de 02_create_tables.sql.

CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_active_name ON products(is_active, name);
CREATE INDEX idx_sales_sale_date ON sales(sale_date);
CREATE INDEX idx_sales_customer_id ON sales(customer_id);
CREATE INDEX idx_sales_user_id ON sales(user_id);
CREATE INDEX idx_sale_details_sale_id ON sale_details(sale_id);
CREATE INDEX idx_sale_details_product_id ON sale_details(product_id);
CREATE INDEX idx_inventory_product_date ON inventory_movements(product_id, created_at DESC);
CREATE INDEX idx_inventory_user_id ON inventory_movements(user_id);
CREATE INDEX idx_users_active ON users(is_active);

CREATE UNIQUE INDEX uq_inventory_sale_detail_sale
    ON inventory_movements(sale_detail_id)
    WHERE movement_type = 'SALE';

CREATE UNIQUE INDEX uq_inventory_sale_detail_reversal
    ON inventory_movements(sale_detail_id)
    WHERE movement_type = 'SALE_REVERSAL';
