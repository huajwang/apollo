package com.goodfeel.nightgrass.web.util;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class CartItemUpdateRequest {
    private Long itemId;
    private Integer quantity;

    // the below is for check box
    private Boolean isChecked;
}
