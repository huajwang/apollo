package com.goodfeel.nightgrass.service;

import com.goodfeel.nightgrass.dto.ProductPhotoDto;
import reactor.core.publisher.Flux;

public interface IProductPhotoService {
    Flux<ProductPhotoDto> findProductImg(Long productId);

}
