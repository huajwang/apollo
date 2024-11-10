package com.goodfeel.nightgrass.repo;

import com.goodfeel.nightgrass.data.ProductPhoto;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ProductPhotoRepo extends ReactiveCrudRepository<ProductPhoto, Long> {
    Flux<ProductPhoto> findAllByProductId(Long productId);
}
