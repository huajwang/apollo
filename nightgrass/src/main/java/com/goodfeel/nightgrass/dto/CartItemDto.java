package com.goodfeel.nightgrass.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class CartItemDto {

    private Long itemId;
    private Long cartId;
    private Long productId;
    private String productName;
    private int quantity;
    private String properties;
    private BigDecimal price;

}

