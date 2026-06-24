INSERT INTO product (code, base_price) VALUES
('P001', 10.50),
('P002', 25.99),
('P003', 7.30),
('P004', 37.20);

-- Sample promotional coupons
INSERT INTO coupon (code, description, discount_percentage, usage_limit, current_usage, expiry_date, status) VALUES
('SAVE10',    '10% Welcome discount',     0.1000, 100, 99,  '2026-12-31 23:59:59', 'ACTIVE'),
('FLASH50',   'Flash sale 50% - limited seats', 0.5000, 100,   4,  '2026-03-01 23:59:59', 'ACTIVE'),
('EXPIRED01', 'Expired promo (demo job)',      0.2000, 50,  50, '2025-01-01 00:00:00', 'ACTIVE');

-- Users
INSERT INTO user_app (id, username, password, vip) VALUES
(1, 'JAY', '$2a$10$tJp.qK6iV3/Fq7W.O3eGGe5O7U8N9R0Q1V2W3X4Y5Z6A7B8C9D0E', true),
(2, 'admin', '$2a$10$FMVYqUjFkc2II1P1H.1h8Oye/dHUpNhNK.4rQwkn2/FsyU5k4iQC.', true);

-- 1. Create the Cart
INSERT INTO cart (id, cart_status)
VALUES (1, 'ACTIVE');

-- 2. Associate the cart with user 1 and session 1 (CartOwnership)
INSERT INTO cart_ownership (id, cart_id, user_id)
VALUES (1, 1, 1);

-- 3. Fundamental: Add at least one product (CartItem)
-- Without this, the calculateTotal method will throw CartEmptyException
INSERT INTO cart_item (id, cart_id, product_id, quantity) VALUES
(1, 1, 2, 1),
(2, 1, 4, 1);

INSERT INTO roles (id, name, description) VALUES
(1, 'USER', 'Standard user access'),
(2, 'ADMIN', 'Full system access');

INSERT INTO user_roles (user_id, role_id) VALUES (1, 2), (1, 1);