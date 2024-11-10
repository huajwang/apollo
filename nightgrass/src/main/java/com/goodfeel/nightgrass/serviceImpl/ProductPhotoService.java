package com.goodfeel.nightgrass.serviceImpl;

import com.goodfeel.nightgrass.dto.ProductPhotoDto;
import com.goodfeel.nightgrass.repo.ProductPhotoRepo;
import com.goodfeel.nightgrass.service.IProductPhotoService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ProductPhotoService implements IProductPhotoService {

    private final ProductPhotoRepo productPhotoRepo;

    public ProductPhotoService(ProductPhotoRepo productPhotoRepo) {
        this.productPhotoRepo = productPhotoRepo;
    }

    @Override
    public Flux<ProductPhotoDto> findProductImg(Long productId) {
        return productPhotoRepo.findAllByProductId(productId).map(productPhoto -> {
            return new ProductPhotoDto(productPhoto.getId(), productPhoto.getProductId(), productPhoto.getPhotoUrl());
        });
    }
}
