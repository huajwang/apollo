package com.goodfeel.nightgrass.serviceImpl;

import com.goodfeel.nightgrass.data.Cart;
import com.goodfeel.nightgrass.data.CartItem;
import com.goodfeel.nightgrass.dto.CartItemDto;
import com.goodfeel.nightgrass.repo.CartItemRepository;
import com.goodfeel.nightgrass.repo.CartRepository;
import com.goodfeel.nightgrass.repo.ProductRepository;
import com.goodfeel.nightgrass.service.ICartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;


@Service
public class CartService implements ICartService {

    private final Logger logger = LoggerFactory.getLogger(CartService.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
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
        return getCurrentUserId()
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
                                            CartItem newItem = new CartItem(null, cart.getCartId(), productId, 1, "blue");
                                            return cartItemRepository.save(newItem).thenReturn(cart);
                                        })
                        )
                        // After adding/updating the item, update the cart total
                        .flatMap(updatedCart -> updateCartTotal(updatedCart.getCartId()).thenReturn(updatedCart))
                );
    }

    @Override
    public Mono<Integer> getCartItemCount() {
        return getCurrentUserId().flatMap(this::getCartForUser).flatMap(cart ->
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
        return getCurrentUserId()
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
                        product.getName(),
                        product.getDescription(),
                        cartItem.getQuantity(),
                        cartItem.getProperties(),
                        product.getPrice(),
                        currencyFormat.format(product.getPrice())
                ));
    }

    @Override
    public Mono<BigDecimal> getTotalPrice() {
        return getCartItems()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Mono<String> getCurrentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .defaultIfEmpty("Guest") // Guest if the user is not logged in
                .doOnSuccess(it -> logger.debug("The current user is {}", it));
    }

    public Mono<CartItem> updateQuantity(Long itemId, int quantity) {
        return cartItemRepository.findById(itemId)
                .flatMap(cartItem -> {
                    cartItem.setQuantity(quantity);
                    return cartItemRepository.save(cartItem)
                            .then(updateCartTotal(cartItem.getCartId())); // Update total after saving
                })
                .then(cartItemRepository.findById(itemId)); // Return updated cart item
    }

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


}
