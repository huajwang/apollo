package com.goodfeel.nightgrass.repo

import com.goodfeel.nightgrass.data.Cart
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono
import java.math.BigDecimal

interface CartRepository : ReactiveCrudRepository<Cart, Long> {
    fun findByUserId(userId: String): Mono<Cart>

    @Query("UPDATE e_mall_cart SET total = :total WHERE cart_id = :cartId")
    fun updateTotal(cartId: Long, total: BigDecimal): Mono<Void>
    fun findByGuestId(guestId: String): Mono<Cart>
    fun deleteByCartId(cartId: Long): Mono<Void>
}
