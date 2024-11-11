package com.goodfeel.nightgrass.service;

import com.goodfeel.nightgrass.data.Cart;
import com.goodfeel.nightgrass.dto.CartItemDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface ICartService {

    Mono<Cart> getCartForUser(String userId);
    Mono<Cart> addProductToCart(Long productId);
    Mono<Integer> getCartItemCount();
    Mono<Void> removeProductFromCart(Long productId);
    Flux<CartItemDto> getCartItems();
    Mono<BigDecimal> getTotalPrice();
}
