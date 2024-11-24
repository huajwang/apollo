package com.goodfeel.nightgrass.repo

import com.goodfeel.nightgrass.data.CartItem
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux

interface CartItemRepository : ReactiveCrudRepository<CartItem, Long> {
    fun findByCartId(cartId: Long): Flux<CartItem>
    fun deleteByCartIdAndIsSelected(cartId: Long, b: Boolean): Flux<Void>
}
