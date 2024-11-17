package com.goodfeel.nightgrass.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class OrderItemDto {

    private String productName;
    private String imageUrl;
    private Integer quantity;
    private String properties;
    private BigDecimal unitPrice;
}
