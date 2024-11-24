package com.goodfeel.nightgrass.service

import com.goodfeel.nightgrass.data.Cart
import com.goodfeel.nightgrass.dto.CartItemDto
import com.goodfeel.nightgrass.web.util.AddCartRequest
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal

interface ICartService {
    fun getCartForUser(userId: String): Mono<Cart>
    fun addProductToCart(addCartRequest: AddCartRequest): Mono<Cart>
    fun getCartItemCount(): Mono<Int>

    fun removeCartItemFromCart(itemId: Long): Mono<Void>
    fun getCartItems(): Flux<CartItemDto>
    fun getTotalPrice(): Mono<BigDecimal>
}
