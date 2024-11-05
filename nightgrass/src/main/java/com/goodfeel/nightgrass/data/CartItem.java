package com.goodfeel.nightgrass.data;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

@Setter
@Getter
public class CartItem {
    @Id
    private Long id;
    private Long productId;
    private int quantity;
}
