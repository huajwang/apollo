package com.goodfeel.nightgrass.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CartItemDto {
    private Long productId;
    private String productName;
    private String description;
    private Double price;
    private int quantity;
}
