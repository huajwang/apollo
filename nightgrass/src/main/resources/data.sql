-- Insert sample data into e_mall_user
INSERT INTO e_mall_user (oauth_id, nick_name, email, customer_name, phone, address) VALUES
('yaojiabuy001', '长风万里', 'huajian.wang@yaojiabuy.com', 'Huajian Wang', '0987654321', '456 Elm St'),
('yaojiabuy002', '欧阳', 'yanfeng.ouyang@yaojiabuy.com', 'OuYang', '1234567890', '123 Main St');

INSERT INTO e_mall_product (product_name, description, image_url, price, additional_info, category) VALUES
('Drone E88', 'Advanced features for smooth flight', '/images/drone.jpg', 50.00, '{"material": "High quality material", "shipping": "Free shipping available", "warranty": "1-year warranty"}', 'NONE'),
('Dash Cam 360', 'Full HD, night vision, and motion detection', '/images/dashcam.png', 80, '{"packaging": "Eco-friendly packaging", "availability": "Limited stock", "return_policy": "30-day return policy"}', 'NONE'),
('Car Accessory Kit', 'All essentials for a safe drive', '/images/car_accessory.webp', 300, '{"battery_life":"35 minutes", "range":"4km"}', 'NONE'),
('Charger', 'Apple Charger', '/images/drone.jpg', 1099, '{"battery_life":"35 minutes", "range":"4km"}', 'NONE'),
('SkyExplorer Pro', 'High-performance drone with 4K camera and long battery life.', '/images/product1.png', 499.99, '{"battery_life":"40 minutes", "range":"5km"}', 'POPULAR'),
('AeroCapture 300', 'Compact drone perfect for beginners and casual users.', '/images/product2.png', 299.99, '{"battery_life":"25 minutes", "range":"3km"}', 'NEW'),
('Nimbus Ultra HD', 'Professional-grade drone with HDR camera and obstacle avoidance.', '/images/product3.png', 999.99, '{"battery_life":"30 minutes", "range":"8km"}', 'POPULAR'),
('FlyBuddy Mini', 'Affordable mini drone for kids and hobbyists.', '/images/product4.png', 49.99, '{"battery_life":"10 minutes", "range":"100m"}', 'POPULAR'),
('WuKong Vision', 'Mid-range drone with advanced tracking features.', '/images/product1.png', 699.99, '{"battery_life":"35 minutes", "range":"4km"}', 'NEW'),
('FlyBird Mini', 'Affordable mini drone for kids and hobbyists.', '/images/product4.png', 49.99, '{"battery_life":"10 minutes", "range":"100m"}', 'POPULAR'),
('BirdEye Vision', 'Mid-range drone with advanced tracking features.', '/images/product1.png', 699.99, '{"battery_life":"35 minutes", "range":"4km"}', 'POPULAR'),

('SkyExplorer Pro 1', 'High-performance drone with 4K camera and long battery life.', '/images/product1.png', 799.99, '{"battery_life":"40 minutes", "range":"5km"}', 'BIG_HIT'),
('AeroCapture 300 1', 'Compact drone perfect for beginners and casual users.', '/images/product2.png', 299.99, '{"battery_life":"25 minutes", "range":"3km"}', 'NEW'),
('Nimbus Ultra HD 1', 'Professional-grade drone with HDR camera and obstacle avoidance.', '/images/product3.png', 999.99, '{"battery_life":"30 minutes", "range":"8km"}', 'BIG_HIT'),
('FlyBuddy Mini 1', 'Affordable mini drone for kids and hobbyists.', '/images/product4.png', 49.99, '{"battery_life":"10 minutes", "range":"100m"}', 'BIG_HIT');


INSERT INTO e_mall_discount (product_id, discount_type, discount_value, start_date, end_date) VALUES
(4, 'PERCENTAGE', 10, '2024-01-01 00:00:00', '2024-12-31 23:59:59'),
(5, 'FLAT', 20, NULL, NULL),
(6, 'PERCENTAGE', 30, '2024-01-01 00:00:00', '2024-12-31 23:59:59'),
(7, 'FLAT', 20, NULL, NULL);


INSERT INTO e_mall_product_photo (product_id, photo_url) VALUES
(1, '/images/lake.jpg'),
(1, '/images/maple.jpg'),
(1, '/images/glass.jpg'),
(2, '/images/drone.jpg');


INSERT INTO e_mall_product_property (product_id, property_name, property_value) VALUES
(1, 'Battery Life', '40 minutes,30 minutes,20 minutes'),
(1, 'Camera Quality', '4K Ultra HD,1080 HD'),
(2, 'Color', 'Red,Blue,Green'),
(2, 'Material', 'Cotton,Silk'),
(3, 'Obstacle Avoidance', 'Yes,No'),
(4, 'Flight Range', '500m,300m'),
(5, 'Color', 'Red,Yellow,Blue');

INSERT INTO e_mall_product_review (product_id, reviewer, content) VALUES
(1, 'Alice', 'This drone is amazing! The camera quality is outstanding.'),
(1, 'Bob', 'Great value for money. Highly recommended for beginners.'),
(2, 'Charlie', 'Compact and easy to use. Perfect for travel.'),
(2, 'Diana', 'Battery life could be better, but overall a great product.'),
(3, 'Edward', 'Professional-grade drone. Worth every penny.'),
(3, 'Fiona', 'Obstacle avoidance is a game-changer. Love it!'),
(4, 'George', 'Affordable and fun for the kids. Easy to operate.'),
(4, 'Helen', 'Good for basic use. Don’t expect professional quality.'),
(5, 'Ian', 'Tracking features are excellent. Great for filming outdoor activities.'),
(5, 'Jenny', 'A bit pricey, but the performance is top-notch.');




-- Insert sample data into e_mall_blog_category
INSERT INTO e_mall_blog_category (slug, name) VALUES
('tech', 'Technology'),
('travel', 'Travel'),
('food', 'Food');

-- Insert sample data into e_mall_blog_posts_media
INSERT INTO e_mall_blog_posts_media (post_id, type, file_path, caption)
VALUES
(1, 'PHOTO', '/media/lake.jpg', 'Tech photo caption'),
(2, 'PHOTO', '/media/maple.jpg', 'Travel photo caption'),
(3, 'PHOTO', '/media/lake.jpg', 'Food photo caption');

-- Insert sample data into e_mall_blog_posts
INSERT INTO e_mall_blog_posts (author_id, title, slug, content, abstract, category_id, status, thumbnail, main_media_id, sticky_pin_no, published_at)
VALUES
('yaojiabuy001', 'Tech Trends 2024', 'tech-trends-2024', 'Content about tech trends', 'A summary of tech trends', 1, 'PUBLISHED', '/images/lake.jpg', 1, 10, NOW()),
('yaojiabuy001', 'Top Travel Destinations', 'top-travel-destinations', 'Content about travel destinations', 'A summary of travel destinations', 2, 'PUBLISHED', '/images/maple.jpg', 2, 5, NOW()),
('yaojiabuy002', 'Best Recipes of 2024', 'best-recipes-2024', 'Content about recipes', 'A summary of recipes', 3, 'PUBLISHED', '/images/lake.jpg', 3, 0, NOW());
