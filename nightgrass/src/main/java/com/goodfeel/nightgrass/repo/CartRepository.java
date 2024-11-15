package com.goodfeel.nightgrass.repo;

import com.goodfeel.nightgrass.data.Cart;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface CartRepository extends ReactiveCrudRepository<Cart, Long> {
    Mono<Cart> findByUserId(String userId);

    @Query("UPDATE e_mall_cart SET total = :total WHERE cart_id = :cartId")
    Mono<Void> updateTotal(Long cartId, BigDecimal total);
}
