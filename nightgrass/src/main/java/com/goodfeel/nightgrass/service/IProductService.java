package com.goodfeel.nightgrass.service;

import com.goodfeel.nightgrass.data.Product;
import com.goodfeel.nightgrass.web.ProductDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IProductService {
    Flux<ProductDto> getAllProducts();
    Mono<ProductDto> getProductById(Long id);
    Mono<Product> saveProduct(Product product);
    Mono<Void> deleteProduct(Long id);
}
