-- Drop order: child tables first (referential integrity)
DROP TABLE IF EXISTS cart_ownership;
DROP TABLE IF EXISTS cart_item;
DROP TABLE IF EXISTS cart;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS user_app;
DROP TABLE IF EXISTS coupon;
DROP TABLE IF EXISTS product;

-- =============================================
-- CORE ENTITIES
-- =============================================

CREATE TABLE user_app (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255),
    vip BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE
);

CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,

    -- Composite primary key prevents duplicates (same role for same user)
    PRIMARY KEY (user_id, role_id),

    -- Constraint referencing the user table
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES user_app(id)
        ON DELETE CASCADE,

    -- Constraint referencing the role table
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE CASCADE
);

CREATE TABLE product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(255) NOT NULL,
    base_price DECIMAL(19, 2) NOT NULL
);

-- =============================================
-- CART SYSTEM
-- =============================================

CREATE TABLE cart (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_status VARCHAR(20)
);

CREATE TABLE cart_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT,
    product_id BIGINT,
    quantity INTEGER NOT NULL,

    CONSTRAINT fk_cart_item_cart FOREIGN KEY (cart_id) REFERENCES cart(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_item_product FOREIGN KEY (product_id) REFERENCES product(id),
    CHECK (quantity >= 1)
);

CREATE TABLE cart_ownership (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT UNIQUE,                 -- at most one open cart per user
    anonymous_id VARCHAR(64) UNIQUE,       -- at most one open cart per guest
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ownership_cart FOREIGN KEY (cart_id) REFERENCES cart(id) ON DELETE CASCADE,
    CONSTRAINT fk_ownership_user FOREIGN KEY (user_id) REFERENCES user_app(id),
    -- exactly one owner: either a registered user or a guest
    CONSTRAINT chk_ownership_single_owner CHECK ((user_id IS NULL) <> (anonymous_id IS NULL))
);

-- =============================================
-- COUPON SYSTEM (with Optimistic Locking)
-- =============================================

CREATE TABLE coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    discount_percentage DECIMAL(5, 4) NOT NULL,
    usage_limit INTEGER NOT NULL,
    current_usage INTEGER NOT NULL DEFAULT 0,
    expiry_date TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,

    CHECK (discount_percentage > 0 AND discount_percentage <= 1),
    CHECK (current_usage >= 0)
);