package com.goodfeel.nightgrass.repo;

import com.goodfeel.nightgrass.data.Cart;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface CartRepo extends ReactiveCrudRepository<Cart, Long> {
    Mono<Cart> findByUserId(String userId);
}
