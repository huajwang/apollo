package com.goodfeel.nightgrass.service;

import com.goodfeel.nightgrass.dto.CartItemDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ICartService {
    Mono<Void> addProductToCart(Long productId, int quantity);
    Mono<Integer> getCartItemCount();
    Mono<Void> removeProductFromCart(Long productId);
    Flux<CartItemDto> getCartItems();
    Mono<Double> getTotalPrice();
}
