package com.goodfeel.nightgrass.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class ProductDto {

    private Long productId;
    private String productName;
    private String description;
    private String imageUrl;
    private BigDecimal price;

}
