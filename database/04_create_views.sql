-- Ejecutar después de crear las tablas e índices.

CREATE VIEW vw_sale_totals AS
SELECT
    s.id AS sale_id,
    s.sale_date,
    s.status,
    s.customer_id,
    s.user_id,
    s.payment_method_id,
    COALESCE(SUM(sd.subtotal), 0)::NUMERIC(14, 2) AS total
FROM sales s
LEFT JOIN sale_details sd ON sd.sale_id = s.id
GROUP BY s.id, s.sale_date, s.status, s.customer_id, s.user_id, s.payment_method_id;

CREATE VIEW vw_product_low_stock AS
SELECT p.id AS product_id, p.name AS product_name, p.stock, p.minimum_stock,
       c.id AS category_id, c.name AS category_name
FROM products p
JOIN categories c ON c.id = p.category_id
WHERE p.is_active = TRUE AND p.stock <= p.minimum_stock;

CREATE VIEW vw_top_selling_products AS
SELECT sd.product_id, sd.product_name,
       SUM(sd.quantity) AS units_sold,
       SUM(sd.subtotal)::NUMERIC(14, 2) AS sales_amount
FROM sale_details sd
JOIN sales s ON s.id = sd.sale_id
WHERE s.status = 'COMPLETED'
GROUP BY sd.product_id, sd.product_name;

CREATE VIEW vw_sales_by_day AS
SELECT (s.sale_date AT TIME ZONE 'America/Bogota')::DATE AS sale_day,
       COUNT(DISTINCT s.id) AS sales_count,
       SUM(sd.subtotal)::NUMERIC(14, 2) AS sales_amount
FROM sales s
JOIN sale_details sd ON sd.sale_id = s.id
WHERE s.status = 'COMPLETED'
GROUP BY (s.sale_date AT TIME ZONE 'America/Bogota')::DATE;
