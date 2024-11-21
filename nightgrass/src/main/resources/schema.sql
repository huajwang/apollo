CREATE TABLE IF NOT EXISTS e_mall_product (
    product_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    image_url VARCHAR(255),
    price DECIMAL(10, 2) NOT NULL
);


CREATE TABLE IF NOT EXISTS e_mall_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    oauth_id VARCHAR(255) UNIQUE NOT NULL,
    nick_name VARCHAR(255),
    email VARCHAR(255),
    customer_name VARCHAR(50),
    phone VARCHAR(15),
    address VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS e_mall_product_photo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    photo_url VARCHAR(255) NOT NULL,
    FOREIGN KEY (product_id) REFERENCES e_mall_product(product_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS e_mall_cart (
    cart_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    total DECIMAL(10, 2) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    introducer VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS e_mall_cart_item (
    item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cart_id BIGINT REFERENCES e_mall_cart(cart_id),
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    properties VARCHAR(255),
    is_selected TINYINT(1) DEFAULT 0,  -- 0 = false, 1 = true
    FOREIGN KEY (cart_id) REFERENCES e_mall_cart(cart_id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES e_mall_product(product_id) ON DELETE CASCADE
);


CREATE TABLE IF NOT EXISTS e_mall_order (
    order_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(30) NOT NULL,
    user_id VARCHAR(255),
    total DECIMAL(10, 2) NOT NULL,
    hst DECIMAL(10, 2),
    final_total DECIMAL(10, 2),
    delivery_address VARCHAR(100),
    contact_name VARCHAR(50),
    contact_phone VARCHAR(15),
    created_at TIMESTAMP NOT NULL,
    introducer VARCHAR(255),
    updated_date TIMESTAMP,
    logistics_no VARCHAR(30),
    delivery_date TIMESTAMP,
    status VARCHAR(10),
    pay_no VARCHAR(20),
    pay_type VARCHAR(10),
    remark VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS e_mall_order_item (
    order_item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT REFERENCES e_mall_order(order_id),
    product_name VARCHAR(100) NOT NULL,
    image_url VARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    properties VARCHAR(255),
    unit_price DECIMAL(10, 2) NOT NULL,  -- price at the time of order
    FOREIGN KEY (order_id) REFERENCES e_mall_order(order_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS e_mall_referral (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sharer_id BIGINT NOT NULL,  -- ID of the sharer (user ID)
    referral_code VARCHAR(255) UNIQUE NOT NULL,  -- Unique referral code
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS e_mall_referral_rewards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sharer_id BIGINT NOT NULL,  -- ID of the sharer (user ID)
    order_id BIGINT NOT NULL,  -- Associated order ID
    reward_amount DECIMAL(10, 2) NOT NULL,  -- reward amount
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES e_mall_order(order_id) ON DELETE CASCADE
);

