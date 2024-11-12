package com.goodfeel.nightgrass.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("e_mall_order_item")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderItem {

    @Id
    private Long orderItemId;
    private Long orderId;
    private Long productId;
    private Integer quantity;
    private String properties;
}
