package com.goodfeel.nightgrass.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

/**
 * Stores individual cart items, linked to the e_mall_cart by cart_id
 */

@Table("e_mall_cart_item")
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class CartItem {

    @Id
    private Long itemId;
    private Long cartId;
    private Long productId;
    private Integer quantity;
    private String properties;
    private BigDecimal price;
}
