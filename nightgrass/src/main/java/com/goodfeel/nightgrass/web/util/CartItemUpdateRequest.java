package com.goodfeel.nightgrass.web.util;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class CartItemUpdateRequest {
    private Long itemId;
    private int quantity;

    // the below is for check box
    private BigDecimal amount;
    private Boolean isChecked;
}
