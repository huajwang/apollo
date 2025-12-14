package com.goodfeel.nightgrass.service

import com.goodfeel.nightgrass.data.Cart
import com.goodfeel.nightgrass.data.CartItem
import com.goodfeel.nightgrass.data.User
import com.goodfeel.nightgrass.dto.CartItemDto
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal

interface ICartService {
    fun getCartForUserOrGuest(user: User?, guestSessionId: String?): Mono<Cart>
    fun getCartItemCount(user: User): Mono<Int>

    fun removeCartItemFromCart(itemId: Long): Mono<Long>
    fun updateQuantity(itemId: Long, quantity: Int): Mono<CartItem>
    fun getCartItemsForCart(cartId: Long): Flux<CartItemDto>
    fun getSubtotal(cartId: Long): Mono<BigDecimal>
    fun getTotalAfterDiscount(cartId: Long): Mono<BigDecimal>
    fun getSavings(cartId: Long): Mono<BigDecimal>
    fun mergeCart(userId: String, guestId: String): Mono<Void>
    fun clearCart(user: User?, guestSessionId: String?): Mono<Void>
}

