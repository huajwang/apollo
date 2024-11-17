package com.goodfeel.nightgrass.serviceImpl;

import com.goodfeel.nightgrass.data.Cart;
import com.goodfeel.nightgrass.data.CartItem;
import com.goodfeel.nightgrass.data.Order;
import com.goodfeel.nightgrass.data.OrderItem;
import com.goodfeel.nightgrass.dto.CartItemDto;
import com.goodfeel.nightgrass.repo.*;
import com.goodfeel.nightgrass.service.ICartService;
import com.goodfeel.nightgrass.web.util.Utility;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;


@Service
public class CartService implements ICartService {

    private final Logger logger = LoggerFactory.getLogger(CartService.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository, ProductRepository productRepository, OrderRepository orderRepository, OrderItemRepository orderItemRepository, UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Mono<Cart> getCartForUser(String userId) {
        if (userId.equalsIgnoreCase("Guest")) {
            // Handle guest cart logic (e.g., use session storage or a temporary in-memory cart)
            return Mono.just(new Cart()); // TODO - retrieve from session storage or local cache in browser?
        }
        return cartRepository.findByUserId(userId)
                .switchIfEmpty(
                        cartRepository.save(new Cart(null, BigDecimal.ZERO, userId, ""))
                );
    }

    @Override
    public Mono<Cart> addProductToCart(Long productId) {
        return Utility.getCurrentUserId()
                .flatMap(this::getCartForUser) // Retrieve or create cart for user
                .flatMap(cart -> cartItemRepository.findByCartId(cart.getCartId())
                        .filter(item -> item.getProductId().equals(productId))
                        .next() // Take the first matching item, if any
                        .flatMap(existingItem -> {
                            // If product exists, increment quantity and save
                            existingItem.setQuantity(existingItem.getQuantity() + 1);
                            return cartItemRepository.save(existingItem).thenReturn(cart);
                        })
                        .switchIfEmpty(
                                // If product does not exist in cart, add as new CartItem
                                productRepository.findById(productId)
                                        .flatMap(product -> {
                                            CartItem newItem = new CartItem(
                                                    null, cart.getCartId(), productId, 1, "blue",
                                                    true);
                                            return cartItemRepository.save(newItem).thenReturn(cart);
                                        })
                        )
                        // After adding/updating the item, update the cart total
                        .flatMap(updatedCart -> updateCartTotal(updatedCart.getCartId()).thenReturn(updatedCart))
                );
    }

    @Override
    public Mono<Integer> getCartItemCount() {
        return Utility.getCurrentUserId().flatMap(this::getCartForUser).flatMap(cart ->
                cartItemRepository.findByCartId(cart.getCartId())
                        .map(CartItem::getQuantity)    // Extract quantity of each item
                        .reduce(0, Integer::sum)

        );
    }

    @Override
    public Mono<Void> removeCartItemFromCart(Long itemId) {
        return cartItemRepository.findById(itemId)
                .flatMap(cartItem -> cartItemRepository.delete(cartItem)
                        .then(updateCartTotal(cartItem.getCartId()))); // Update total after deletion
    }

    @Override
    public Flux<CartItemDto> getCartItems() {
        return Utility.getCurrentUserId()
                .flatMap(this::getCartForUser)
                .flatMapMany(cart -> cartItemRepository.findByCartId(cart.getCartId()))
                .flatMap(this::mapToCartItemDto);
    }

    private Mono<CartItemDto> mapToCartItemDto(CartItem cartItem) {
        // Format price in Canadian dollars
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.CANADA);
        return productRepository.findById(cartItem.getProductId())
                .map(product -> new CartItemDto(
                        cartItem.getItemId(),
                        cartItem.getCartId(),
                        cartItem.getProductId(),
                        product.getImageUrl(),
                        product.getProductName(),
                        product.getDescription(),
                        cartItem.getQuantity(),
                        cartItem.getProperties(),
                        product.getPrice(),
                        currencyFormat.format(product.getPrice()),
                        cartItem.getIsSelected()
                ));
    }

    @Override
    public Mono<BigDecimal> getTotalPrice() {
        return getCartItems()
                .filter(CartItemDto::getIsSelected) // Only include items that are selected
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    public Mono<CartItem> updateQuantity(Long itemId, int quantity) {
        return cartItemRepository.findById(itemId)
                .flatMap(cartItem -> {
                    cartItem.setQuantity(quantity);
                    return cartItemRepository.save(cartItem)
                            //  ensure that the total is only updated when a cart item is selected
                            .then(cartItem.getIsSelected() ? updateCartTotal(cartItem.getCartId()) : Mono.empty());
                })
                .then(cartItemRepository.findById(itemId)); // Return updated cart item
    }

    /**
     * This item is used to update the cart total when
     * 1. A product is added to the shopping cart;
     * 2. quantity is changed;
     * 3. cart item is deleted from the cart;
     *
     * @param cartId
     * @return
     */
    private Mono<Void> updateCartTotal(Long cartId) {
        // Retrieve all items in the cart
        return cartItemRepository.findByCartId(cartId)
                .flatMap(cartItem -> productRepository.findById(cartItem.getProductId())
                        .map(product -> product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()))))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .flatMap(total -> cartRepository.findById(cartId)
                        .flatMap(cart -> {
                            cart.setTotal(total);
                            return cartRepository.save(cart);
                        }))
                .then();
    }

    /**
     * This function is used to update the cart total when user select/deselect the item by (un)check the checkbox
     *
     * @param itemId     - Cart item ID
     * @param isSelected Is the cart item selected or not
     * @return
     */
    public Mono<Void> updateCartTotal(Long itemId, boolean isSelected) {
        return cartItemRepository.findById(itemId)
                .flatMap(cartItem -> {
                    cartItem.setIsSelected(isSelected);
                    return cartItemRepository.save(cartItem);
                })
                .flatMap(cartItem -> productRepository.findById(cartItem.getProductId())
                        .map(product -> {
                            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
                            return new CartTotalUpdate(cartItem.getCartId(), itemTotal);
                        }))
                .flatMap(cartTotalUpdate -> cartRepository.findById(cartTotalUpdate.getCartId())
                        .flatMap(cart -> {
                            BigDecimal newTotal = isSelected ? cart.getTotal().add(cartTotalUpdate.getItemTotal())
                                    : cart.getTotal().subtract(cartTotalUpdate.getItemTotal());
                            cart.setTotal(newTotal);
                            return cartRepository.save(cart);
                        }))
                .then();
    }

    // Helper class to carry cart ID and item total to avoid recalculations
    @Getter
    private static class CartTotalUpdate {
        private final Long cartId;
        private final BigDecimal itemTotal;

        public CartTotalUpdate(Long cartId, BigDecimal itemTotal) {
            this.cartId = cartId;
            this.itemTotal = itemTotal;
        }

    }

    /**
     * When something is wrong when doing checkout the shopping cart, it needs to perform DB rollback.
     *
     * @param userId
     * @return
     */
    @Transactional
    public Mono<Order> createOrderAndCleanupShoppingCart(String userId) {
        return cartRepository.findByUserId(userId)
                .flatMap(cart -> {
                    Flux<CartItemDto> cartItemDtoFlux = cartItemRepository.findByCartId(cart.getCartId())
                            .filter(CartItem::getIsSelected)
                            .flatMap(this::populateCartItemWithProductInfo);

                    return createOrder(cart, cartItemDtoFlux)
                            .flatMap(order -> clearCartItemsAndTotal(cart).thenReturn(order));
                });
    }


    private Mono<CartItemDto> populateCartItemWithProductInfo(CartItem cartItem) {
        return productRepository.findById(cartItem.getProductId())
                .map(product -> {
                    CartItemDto cartItemDto = new CartItemDto();
                    cartItemDto.setItemId(cartItem.getItemId());
                    cartItemDto.setProductId(cartItem.getProductId());
                    cartItemDto.setProductName(product.getProductName());
                    cartItemDto.setImageUrl(product.getImageUrl());
                    cartItemDto.setQuantity(cartItem.getQuantity());
                    cartItemDto.setProperties(cartItem.getProperties());
                    cartItemDto.setPrice(product.getPrice()); // Price is fetched from Product
                    return cartItemDto;
                });
    }

    private Mono<Order> createOrder(Cart cart, Flux<CartItemDto> cartItemDtoFlux) {
        // Calculate total reactively from Flux<CartItemDto>
        Mono<BigDecimal> totalMono = cartItemDtoFlux
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return userRepository.findByOauthId(cart.getUserId())// TODO - save userId or OauthId in cart/order table?
                .zipWith(totalMono, (user, total) -> {
                    // Create the order
                    Order order = new Order();
                    order.setOrderNo(generateOrderNo());
                    order.setUserId(cart.getUserId());
                    order.setCreatedAt(LocalDateTime.now());
                    order.setIntroducer(cart.getIntroducer());
                    order.setTotal(total);

                    // Copy user details to order
                    order.setContactName(user.getCustomerName());
                    order.setContactPhone(user.getPhone());
                    order.setDeliveryAddress(user.getAddress());

                    logger.debug("Creating order for user {}, orderNo = {}, total = {}, introducer = {}",
                            order.getUserId(), order.getOrderNo(), order.getTotal(), order.getIntroducer());

                    return order;
                })
                .flatMap(order ->
                        // Save the order and create order items in a reactive chain
                        orderRepository.save(order)
                                .flatMap(savedOrder ->
                                        cartItemDtoFlux
                                                .flatMap(cartItemDto -> createOrderItem(savedOrder, cartItemDto))
                                                .then(Mono.just(savedOrder))
                                )
                );
    }

    private Mono<OrderItem> createOrderItem(Order order, CartItemDto cartItemDto) {
        // Map CartItem to OrderItem
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(order.getOrderId());
        orderItem.setProductName(cartItemDto.getProductName());
        orderItem.setImageUrl(cartItemDto.getImageUrl());
        orderItem.setQuantity(cartItemDto.getQuantity());
        orderItem.setProperties(cartItemDto.getProperties());
        orderItem.setUnitPrice(cartItemDto.getPrice());

        // Save OrderItem
        return orderItemRepository.save(orderItem);
    }

    /**
     * After user press Checkout button, all selected cart items are all deleted. The rest of cart items, if there is
     * any, are 'unselected'. So, just write cart total as zero.
     *
     * @param cart
     * @return
     */
    private Mono<Void> clearCartItemsAndTotal(Cart cart) {
        // Delete selected cart items and reset cart total
        return cartItemRepository.deleteByCartIdAndIsSelected(cart.getCartId(), true)
                .then(cartRepository.updateTotal(cart.getCartId(), BigDecimal.ZERO))
                .then();
    }

    // Helper method to generate a human-readable order ID with date/time and a unique suffix
    private String generateOrderNo() {
        // Format current date-time to a string
        String dateTimePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        // Generate a random 4-digit number as a suffix to ensure uniqueness
        int randomSuffix = ThreadLocalRandom.current().nextInt(1000, 9999);
        return dateTimePart + randomSuffix;
    }

}
