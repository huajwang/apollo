CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    oauth_id VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255),
    email VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS e_mall_product_photo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    photo_url VARCHAR(255) NOT NULL
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
    FOREIGN KEY (cart_id) REFERENCES e_mall_cart(cart_id) ON DELETE CASCADE
);


CREATE TABLE IF NOT EXISTS e_mall_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id VARCHAR(10) NOT NULL,
    user_id VARCHAR(255),
    amount DECIMAL(10, 2) NOT NULL,
    delivery_address VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    introducer VARCHAR(255),
    order_process_date TIMESTAMP,
    logistics_no VARCHAR(30),
    delivery_date TIMESTAMP,
    order_status VARCHAR(10),
    pay_no VARCHAR (20),
    pay_type VARCHAR(10),
    remark VARCHAR(255)
);
