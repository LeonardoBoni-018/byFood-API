-- Código público e não sequencial para acompanhamento do pedido.
-- Evita que qualquer pessoa enumere /public/orders/{id} e veja dados de outros clientes.
ALTER TABLE customer_order ADD COLUMN tracking_code UUID;

UPDATE customer_order SET tracking_code = gen_random_uuid() WHERE tracking_code IS NULL;

ALTER TABLE customer_order ALTER COLUMN tracking_code SET NOT NULL;

CREATE UNIQUE INDEX ux_customer_order_tracking_code ON customer_order (tracking_code);

-- Listagens do cardápio público filtram por disponibilidade e ordenam por categoria/nome
CREATE INDEX idx_menu_item_available_category_name ON menu_item (available, category, name);
