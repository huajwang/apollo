package com.goodfeel.nightgrass.repo;

import com.goodfeel.nightgrass.data.Product;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductRepo extends ReactiveCrudRepository<Product, Long> {
    Flux<Product> findByNameContaining(String name);
}
