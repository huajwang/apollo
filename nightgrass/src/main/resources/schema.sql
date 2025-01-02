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

CREATE TABLE IF NOT EXISTS e_mall_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    oauth_id VARCHAR(255) UNIQUE,
    guest_id VARCHAR(255) UNIQUE,
    nick_name VARCHAR(255),
    email VARCHAR(255),
    customer_name VARCHAR(50),
    phone VARCHAR(15),
    address VARCHAR(255)
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
    guest_id VARCHAR(255) UNIQUE
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
    pay_type VARCHAR(10),
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

CREATE TABLE IF NOT EXISTS e_mall_blog_category (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS e_mall_blog_posts_media (
    media_id INT AUTO_INCREMENT PRIMARY KEY,
    post_id INT NOT NULL,
    type ENUM('PHOTO', 'AUDIO', 'VIDEO') NOT NULL, -- Type of media
    file_path VARCHAR(255) NOT NULL, -- Path to the media file
    caption VARCHAR(255),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    -- FOREIGN KEY (post_id) REFERENCES e_mall_blog_posts(post_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS e_mall_blog_posts (
    post_id INT AUTO_INCREMENT PRIMARY KEY,
    author_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    content TEXT NOT NULL,
    abstract VARCHAR(512),
    category_id INT NOT NULL,
    status ENUM('DRAFT', 'PUBLISHED', 'ARCHIVED') DEFAULT 'DRAFT',
    thumbnail VARCHAR(255) NOT NULL,
    main_media_id INT NOT NULL,
    sticky_pin_no INT DEFAULT 0, -- If greater than 0 then show on blog home page
    show_on_homepage TINYINT(1) DEFAULT 0, -- If no workshop/event, a chosen blog post will be shown
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    published_at TIMESTAMP NULL DEFAULT NULL,
    FOREIGN KEY (author_id) REFERENCES e_mall_user(oauth_id),
    FOREIGN KEY (category_id) REFERENCES e_mall_blog_category(category_id),
    FOREIGN KEY (main_media_id) REFERENCES e_mall_blog_posts_media(media_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
