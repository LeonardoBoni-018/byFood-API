-- Multi-loja: usuários (cliente/loja), lojas com dono e slug público, cardápio e pedidos por loja.

-- ---------------------------------------------------------------- usuários
CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    login         VARCHAR(120) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    name          VARCHAR(120) NOT NULL,
    phone         VARCHAR(30),
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,
    CONSTRAINT ck_app_user_role CHECK (role IN ('CUSTOMER', 'STORE'))
);

CREATE UNIQUE INDEX ux_app_user_login ON app_user (login);

-- O admin existente passa a ser o dono da loja já cadastrada
INSERT INTO app_user (login, password_hash, name, role, created_at, updated_at)
SELECT username, password_hash, 'Administrador', 'STORE', created_at, updated_at
FROM admin_user;

DROP TABLE admin_user;

-- ------------------------------------------------------------------- lojas
ALTER TABLE restaurant
    ADD COLUMN slug     VARCHAR(80),
    ADD COLUMN owner_id BIGINT REFERENCES app_user (id),
    ADD COLUMN cuisine  VARCHAR(60),
    ADD COLUMN image_url VARCHAR(500),
    ADD COLUMN active   BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE restaurant
SET slug     = 'bistro-byfood',
    cuisine  = 'Variada',
    owner_id = (SELECT id FROM app_user WHERE login = 'admin')
WHERE id = (SELECT MIN(id) FROM restaurant);

UPDATE restaurant SET slug = 'loja-' || id WHERE slug IS NULL;
UPDATE restaurant SET cuisine = 'Variada' WHERE cuisine IS NULL;

ALTER TABLE restaurant ALTER COLUMN slug SET NOT NULL;
ALTER TABLE restaurant ALTER COLUMN cuisine SET NOT NULL;

CREATE UNIQUE INDEX ux_restaurant_slug  ON restaurant (slug);
CREATE UNIQUE INDEX ux_restaurant_owner ON restaurant (owner_id) WHERE owner_id IS NOT NULL;
CREATE INDEX idx_restaurant_active_cuisine ON restaurant (active, cuisine);

-- ---------------------------------------------------------------- cardápio
ALTER TABLE menu_item ADD COLUMN restaurant_id BIGINT;
UPDATE menu_item SET restaurant_id = (SELECT MIN(id) FROM restaurant);
ALTER TABLE menu_item ALTER COLUMN restaurant_id SET NOT NULL;
ALTER TABLE menu_item ADD CONSTRAINT fk_menu_item_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurant (id);

DROP INDEX IF EXISTS idx_menu_item_available_category_name;
CREATE INDEX idx_menu_item_restaurant_menu ON menu_item (restaurant_id, available, category, name);

-- ----------------------------------------------------------------- pedidos
ALTER TABLE customer_order ADD COLUMN restaurant_id BIGINT;
ALTER TABLE customer_order ADD COLUMN customer_id BIGINT;
UPDATE customer_order SET restaurant_id = (SELECT MIN(id) FROM restaurant);
ALTER TABLE customer_order ALTER COLUMN restaurant_id SET NOT NULL;
ALTER TABLE customer_order ADD CONSTRAINT fk_order_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurant (id);
ALTER TABLE customer_order ADD CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES app_user (id) ON DELETE SET NULL;

CREATE INDEX idx_order_restaurant_created ON customer_order (restaurant_id, created_at DESC);
CREATE INDEX idx_order_customer_created   ON customer_order (customer_id, created_at DESC) WHERE customer_id IS NOT NULL;
