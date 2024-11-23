package com.goodfeel.nightgrass.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.Map;

@Table("e_mall_order_item")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderItem {

    @Id
    private Long orderItemId;
    private Long orderId;
    private String productName;
    private String imageUrl;
    private Integer quantity;
    private Map<String, String> properties;
    private BigDecimal unitPrice;
}
