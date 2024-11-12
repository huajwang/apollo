package com.goodfeel.nightgrass.web.util;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CartItemUpdateRequest {
    private Long itemId;
    private int quantity;
}
