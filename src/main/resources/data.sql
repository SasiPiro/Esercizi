INSERT INTO product (code, base_price) VALUES
('P001', 10.50),
('P002', 25.99),
('P003', 7.30),
('P004', 37.20);

-- Coupon promozionali di esempio
INSERT INTO coupon (code, description, discount_percentage, usage_limit, current_usage, expiry_date, status) VALUES
('SAVE10',    'Sconto 10% di benvenuto',     0.1000, 100, 99,  '2026-12-31 23:59:59', 'ACTIVE'),
('FLASH50',   'Flash sale 50% - posti limitati', 0.5000, 100,   4,  '2026-03-01 23:59:59', 'ACTIVE'),
('EXPIRED01', 'Promo scaduta (demo job)',      0.2000, 50,  50, '2025-01-01 00:00:00', 'ACTIVE');

-- User
INSERT INTO user_app (id, username, vip) VALUES (1, 'JAY', true);

-- 1. Crea il Carrello (Cart)
INSERT INTO cart (id, cart_status)
VALUES (1, 'ACTIVE');

-- 2. Lega il carrello all'utente 1 e alla sessione 1 (CartOwnership)
INSERT INTO cart_ownership (id, cart_id, user_id, session_id)
VALUES (1, 1, 1, null);

-- 3. Fondamentale: Aggiungi almeno un prodotto (CartItem)
-- Senza questo, il tuo metodo calculateTotal lancerà CartEmptyException
INSERT INTO cart_item (id, cart_id, product_id, quantity) VALUES
(1, 1, 2, 1),
(2, 1, 4, 1);
