-- Each microservice gets its own isolated database
-- This enforces the "database per service" pattern

CREATE DATABASE product_db;
CREATE DATABASE order_db;
CREATE DATABASE notification_db;

GRANT ALL PRIVILEGES ON DATABASE product_db TO ecommerce;
GRANT ALL PRIVILEGES ON DATABASE order_db TO ecommerce;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO ecommerce;
