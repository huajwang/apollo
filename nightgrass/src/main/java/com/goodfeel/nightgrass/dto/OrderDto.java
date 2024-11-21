package com.goodfeel.nightgrass.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class OrderDto {
    private Long orderId;
    private String orderNo;
    private String userId;
    private String deliveryAddress;
    private BigDecimal orderTotal;
    private Instant createdAt;
    private LocalDateTime orderProcessDate;
    private String logisticsNo;
    private LocalDateTime deliveryDate;
    private String orderStatus;
    private String payNo;
    private String payType;
    private String remark;
}
