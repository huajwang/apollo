package com.goodfeel.nightgrass.serviceImpl;

import com.goodfeel.nightgrass.data.Cart;
import com.goodfeel.nightgrass.data.CartItem;
import com.goodfeel.nightgrass.dto.CartItemDto;
import com.goodfeel.nightgrass.repo.CartItemRepo;
import com.goodfeel.nightgrass.repo.CartRepo;
import com.goodfeel.nightgrass.repo.ProductRepo;
import com.goodfeel.nightgrass.service.ICartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Case 1. user is logged in
 * when user logs in, retrieve cart from db, show item account besides the shopping cart icon, when add item to cart,
 * search if there is an existing item in the cart, if yes, add quantity. else create a cart item and update it in db.
 * Whenever there is update in the shopping cart, e.g. add a new item, delete an item, update the quantity, the db need to be
 * updated.
 *
 *
 *
 * case 2. user is not logged in, but has an account; first he adds products into his local cart. Then he logged in,
 * then the db cart is retrieved. This leads to 2 carts. need consolidate them. Incorporate the local cart items into
 * the db cart.
 * case 3. user does not have an account;
 */
@Service
public class CartService implements ICartService {

    private final Logger logger = LoggerFactory.getLogger(CartService.class);

    private final CartRepo cartRepo;
    private final CartItemRepo cartItemRepo;
    private final ProductRepo productRepo;
    private final Map<Long, CartItemDto> localCart = new HashMap<>();

    public CartService(CartRepo cartRepo, CartItemRepo cartItemRepo, ProductRepo productRepo) {
        this.cartRepo = cartRepo;
        this.cartItemRepo = cartItemRepo;
        this.productRepo = productRepo;
    }

    @Override
    public Mono<Cart> getCartForUser(String userId) {
        if (userId.equalsIgnoreCase("Guest")) {
            // Handle guest cart logic (e.g., use session storage or a temporary in-memory cart)
            return Mono.just(new Cart()); // TODO - retrieve from session storage or local cache in browser?
        }
        return cartRepo.findByUserId(userId)
                .switchIfEmpty(
                    cartRepo.save(new Cart(null, BigDecimal.ZERO, userId, ""))
                );
    }

    @Override
    public Mono<Cart> addProductToCart(Long productId) {
        return getCurrentUserId().flatMap(this::getCartForUser)
        // Retrieve the cart and its items asynchronously
                .flatMap(cart -> {
                    // Check if the product already exists in the cart asynchronously
                    return cartItemRepo.findByCartId(cart.getCartId())
                            .filter(item -> item.getProductId().equals(productId))  // Filter to find matching item
                            .next()  // Take the first match, if any
                            .flatMap(existingItem -> {
                                // If product exists, increment quantity and save the updated item
                                existingItem.setQuantity(existingItem.getQuantity() + 1);
                                return cartItemRepo.save(existingItem)
                                        .then(Mono.just(cart)); // Return cart after saving item
                            })
                            .switchIfEmpty(
                                    productRepo.findById(productId).flatMap(product ->
                                                    // If product does not exist, add it as a new CartItem
                                                    cartItemRepo.save(new CartItem(null, cart.getCartId(),
                                                                    productId, 1, "blue", product.getPrice()))
                                                            .then(Mono.just(cart)) // Return cart after adding new item
                                    )

                            );
                })
                // Save the cart itself after processing items
                .flatMap(cartRepo::save); // Save the updated cart

    }

    @Override
    public Mono<Integer> getCartItemCount() {
        Integer totalCount = localCart.values().stream() // TODO blocking?
                .mapToInt(CartItemDto::getQuantity)
                .sum();
        return Mono.just(totalCount);
    }

    @Override
    public Mono<Void> removeProductFromCart(Long productId) {
        return Mono.fromRunnable(() -> {
            localCart.remove(productId);
        });
    }

    @Override
    public Flux<CartItemDto> getCartItems() {
        return Flux.fromIterable(localCart.values());
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

}
