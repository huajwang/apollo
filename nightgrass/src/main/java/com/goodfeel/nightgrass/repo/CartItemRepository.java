package com.goodfeel.nightgrass.repo;

import com.goodfeel.nightgrass.data.CartItem;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CartItemRepository extends ReactiveCrudRepository<CartItem, Long> {
    Flux<CartItem> findByCartId(Long cartId);
    Mono<Void> deleteByCartIdAndProductId(Long cartId, Long productId);

    Flux<Void> deleteByCartIdAndIsSelected(Long cartId, boolean b);
}