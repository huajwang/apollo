package com.goodfeel.nightgrass.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.math.BigDecimal;

@Table("e_mall_cart")
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class Cart {

    @Id
    private Long cartId;
    private BigDecimal total;
    private String userId;
}
