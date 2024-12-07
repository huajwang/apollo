package com.goodfeel.nightgrass.service

import com.goodfeel.nightgrass.data.Cart
import com.goodfeel.nightgrass.dto.CartItemDto
import com.goodfeel.nightgrass.web.util.AddCartRequest
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal

interface ICartService {
    fun getCartForUserOrGuest(userId: String?, guestId: String?): Mono<Cart>
    fun addProductToCart(addCartRequest: AddCartRequest, userId: String?, guestId: String?): Mono<Cart>
    fun getCartItemCount(userId: String?, guestId: String?): Mono<Int>

    fun removeCartItemFromCart(itemId: Long): Mono<Long>
    fun getCartItemsForCart(cartId: Long): Flux<CartItemDto>
    fun getTotalPriceForCart(cartId: Long): Mono<BigDecimal>
    fun mergeCart(userId: String, guestId: String): Mono<Void>
}
