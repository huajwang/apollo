package com.goodfeel.nightgrass.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("e_mall_product")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Product {

    @Id
    private Long productId;
    private String productName;
    private String description;
    private String imageUrl; // small image. other photos store in another table
    private BigDecimal price;
}
