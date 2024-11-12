package com.goodfeel.nightgrass.serviceImpl;

import com.goodfeel.nightgrass.data.Product;
import com.goodfeel.nightgrass.repo.ProductRepository;
import com.goodfeel.nightgrass.service.IProductService;
import com.goodfeel.nightgrass.dto.ProductDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ProductService implements IProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Flux<ProductDto> getAllProducts() {

        Flux<Product> products = productRepository.findAll();
        return products.map(product ->
                new ProductDto(
                        product.getId(),
                        product.getName(), product.getDescription(), product.getImageUrl(), product.getPrice())
        );
    }

    @Override
    public Mono<ProductDto> getProductById(Long id) {
        return productRepository.findById(id).map(product -> {
            return new ProductDto(product.getId(), product.getName(),
                    product.getDescription(), product.getImageUrl(), product.getPrice());
        });
    }

    @Override
    public Mono<Product> saveProduct(Product product) {
        return productRepository.save(product);
    }

    @Override
    public Mono<Void> deleteProduct(Long id) {
        return productRepository.deleteById(id);
    }

}
