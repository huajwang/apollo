CREATE TABLE IF NOT EXISTS e_mall_product (
    product_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    image_url VARCHAR(255),
    price DECIMAL(10, 2) NOT NULL,
    additional_info JSON,
    category ENUM('BIG_HIT', 'POPULAR', 'NEW', 'NONE') DEFAULT 'NONE'
);

CREATE TABLE IF NOT EXISTS e_mall_discount (
    discount_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    discount_type ENUM('PERCENTAGE', 'FLAT') NOT NULL,
    discount_value DECIMAL(10, 2) NOT NULL,
    start_date DATETIME DEFAULT NULL,
    end_date DATETIME DEFAULT NULL,
    FOREIGN KEY (product_id) REFERENCES e_mall_product(product_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS e_mall_product_review (
    review_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    reviewer VARCHAR(50) NOT NULL,
    content VARCHAR(512) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES e_mall_product(product_id)
);

CREATE TABLE IF NOT EXISTS e_mall_product_property (
    property_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    property_name VARCHAR(255) NOT NULL,
    property_value VARCHAR(255) NOT NULL,
    FOREIGN KEY (product_id) REFERENCES e_mall_product(product_id)
);

CREATE TABLE IF NOT EXISTS e_mall_product_video (
    video_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    video_type ENUM('FILE', 'YOUTUBE', 'VIMEO') NOT NULL DEFAULT 'FILE',
    video_url VARCHAR(255) NOT NULL,
    order_index INT NOT NULL DEFAULT 0, -- Order of the video
    FOREIGN KEY (product_id) REFERENCES e_mall_product(product_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS e_mall_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    oauth_id VARCHAR(255) UNIQUE,
    guest_id VARCHAR(255) UNIQUE,
    nick_name VARCHAR(255),
    email VARCHAR(255),
    avatar VARCHAR(100),
    provider VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS e_mall_address (
    address_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    customer_name VARCHAR(50),
    phone VARCHAR(15),
    address_line VARCHAR(255),
    city VARCHAR(100),
    postal_code VARCHAR(20),
    is_default BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES e_mall_user(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS e_mall_admin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- Store hashed password
    role VARCHAR(50) DEFAULT 'ADMIN'
);

CREATE TABLE IF NOT EXISTS e_mall_workshops (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    date DATE NOT NULL,
    time_start TIME NOT NULL,
    time_end TIME NOT NULL,
    location VARCHAR(255) NOT NULL,
    activities TEXT,
    show_on_homepage TINYINT(1) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
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
    user_id VARCHAR(255) UNIQUE,
    guest_id VARCHAR(255) UNIQUE,
    FOREIGN KEY (user_id) REFERENCES e_mall_user(oauth_id) ON DELETE CASCADE,
    FOREIGN KEY (guest_id) REFERENCES e_mall_user(guest_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS e_mall_cart_item (
    item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cart_id BIGINT REFERENCES e_mall_cart(cart_id),
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    properties JSON,  -- JSON type for storing properties
    is_selected TINYINT(1) DEFAULT 0,  -- 0 = false, 1 = true
    FOREIGN KEY (cart_id) REFERENCES e_mall_cart(cart_id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES e_mall_product(product_id) ON DELETE CASCADE
);


CREATE TABLE IF NOT EXISTS e_mall_order (
    order_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(30) NOT NULL,
    user_id VARCHAR(255),
    original_total DECIMAL(10, 2) NOT NULL,
    discounted_total DECIMAL(10, 2) NOT NULL,
    hst DECIMAL(10, 2) NOT NULL,
    shipping_fee DECIMAL(10, 2) NOT NULL,
    final_total DECIMAL(10, 2),
    delivery_address VARCHAR(100),
    contact_name VARCHAR(50),
    contact_phone VARCHAR(15),
    created_at TIMESTAMP NOT NULL,
    order_status ENUM('PENDING', 'PROCESSING', 'SHIPPING', 'CANCELED', 'COMPLETED') NOT NULL,
    updated_date TIMESTAMP NULL DEFAULT NULL,
    logistics_no VARCHAR(30),
    delivery_date TIMESTAMP NULL DEFAULT NULL,
    pay_no VARCHAR(20),
    pay_type VARCHAR(50),
    remark VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS e_mall_order_item (
    order_item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT REFERENCES e_mall_order(order_id),
    product_name VARCHAR(100) NOT NULL,
    image_url VARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    properties JSON,
    unit_price DECIMAL(10, 2) NOT NULL,  -- price at the time of order
    FOREIGN KEY (order_id) REFERENCES e_mall_order(order_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS e_mall_referral (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sharer_id VARCHAR(255) NOT NULL,  -- ID of the sharer (user ID)
    referral_code VARCHAR(255) UNIQUE NOT NULL,  -- Unique referral code
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sharer_id) REFERENCES e_mall_user(oauth_id)
);

CREATE TABLE IF NOT EXISTS e_mall_referral_rewards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sharer_id VARCHAR(255) NOT NULL,  -- ID of the sharer (user ID)
    order_id BIGINT NOT NULL,  -- Associated order ID
    reward_amount DECIMAL(10, 2) NOT NULL,  -- reward amount
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    referral_reward_status ENUM('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED', 'CANCELED', 'CLAIMED') NOT NULL,
    FOREIGN KEY (order_id) REFERENCES e_mall_order(order_id) ON DELETE CASCADE,
    FOREIGN KEY (sharer_id) REFERENCES e_mall_user(oauth_id)
);
