package com.goodfeel.nightgrass.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("e_mall_product_photo")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductPhoto {
    @Id
    private Long id;
    private Long productId;
    private String photoUrl;
}
