package com.goodfeel.nightgrass.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Table("e_mall_order")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Order {

    @Id
    private Long id;
    private String orderId;
    private String userId;
    private String deliveryAddress;
    private BigDecimal amount;
    private Instant createdAt;
    private String introducer;
    private LocalDateTime orderProcessDate;
    private String logisticsNo;
    private LocalDateTime deliveryDate;
    private String orderStatus;
    private String payNo;
    private String payType;
    private String remark;

}