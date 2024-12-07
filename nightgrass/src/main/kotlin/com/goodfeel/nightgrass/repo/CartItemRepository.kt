package com.goodfeel.nightgrass.repo

import com.goodfeel.nightgrass.data.CartItem
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface CartItemRepository : ReactiveCrudRepository<CartItem, Long> {
    fun findByCartId(cartId: Long): Flux<CartItem>
    fun deleteByCartIdAndIsSelected(cartId: Long, b: Boolean): Flux<Void>
    fun findByCartIdAndProductId(cartId: Long, productId: Long): Flux<CartItem>
    @Query("UPDATE e_mall_cart_item SET quantity = :quantity WHERE item_id = :itemId")
    fun updateQuantity(@Param("itemId") itemId: Long, @Param("quantity") quantity: Int): Mono<CartItem>
}
