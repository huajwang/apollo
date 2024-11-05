package com.goodfeel.nightgrass.serviceImpl;

import com.goodfeel.nightgrass.data.Product;
import com.goodfeel.nightgrass.repo.ProductRepo;
import com.goodfeel.nightgrass.service.IProductService;
import com.goodfeel.nightgrass.web.ProductDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ProductService implements IProductService {

    private final ProductRepo productRepo;

    public ProductService(ProductRepo productRepo) {
        this.productRepo = productRepo;
    }

    @Override
    public Flux<ProductDto> getAllProducts() {

        Flux<Product> products = productRepo.findAll();
        return products.map(product ->
                new ProductDto(
                        product.getId(),
                        product.getName(), product.getDescription(), product.getImageUrl(), product.getPrice())
        );
    }

    @Override
    public Mono<ProductDto> getProductById(Long id) {
        return productRepo.findById(id).map(product -> {
            return new ProductDto(product.getId(), product.getName(),
                    product.getDescription(), product.getImageUrl(), product.getPrice());
        });
    }

    @Override
    public Mono<Product> saveProduct(Product product) {
        return productRepo.save(product);
    }

    @Override
    public Mono<Void> deleteProduct(Long id) {
        return productRepo.deleteById(id);
    }

}
