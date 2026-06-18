-- V1__initial_schema.sql
-- Flyway runs this automatically on startup if the schema doesn't exist yet

CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    sku         VARCHAR(50)     NOT NULL UNIQUE,
    name        VARCHAR(255)    NOT NULL,
    description TEXT,
    price       NUMERIC(10, 2)  NOT NULL CHECK (price >= 0),
    stock       INTEGER         NOT NULL DEFAULT 0 CHECK (stock >= 0),
    image_url   VARCHAR(500),
    active      BOOLEAN         NOT NULL DEFAULT TRUE,
    category_id BIGINT          REFERENCES categories(id) ON DELETE SET NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- Indexes for common queries
CREATE INDEX idx_products_category  ON products(category_id);
CREATE INDEX idx_products_active    ON products(active);
CREATE INDEX idx_products_sku       ON products(sku);

-- Seed data for development
INSERT INTO categories (name, description) VALUES
    ('Electronics', 'Electronic devices and accessories'),
    ('Clothing',    'Apparel and fashion'),
    ('Books',       'Books and educational materials');

INSERT INTO products (sku, name, description, price, stock, category_id) VALUES
    ('ELEC-001', 'Wireless Headphones', 'Noise-cancelling over-ear headphones', 149.99, 50,
        (SELECT id FROM categories WHERE name = 'Electronics')),
    ('ELEC-002', 'USB-C Hub',           '7-in-1 USB-C multiport adapter',        49.99, 120,
        (SELECT id FROM categories WHERE name = 'Electronics')),
    ('CLTH-001', 'Organic Cotton T-Shirt', 'Sustainable basic tee',              29.99, 200,
        (SELECT id FROM categories WHERE name = 'Clothing'));
