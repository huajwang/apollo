package com.goodfeel.nightgrass.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("e_mall_order")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Order {

    @Id
    private Long orderId;
    private String orderNo;
    private String userId;
    private String deliveryAddress;
    private String contactName;
    private String contactPhone;
    private BigDecimal total;
    private BigDecimal hst;
    private BigDecimal finalTotal;
    private String status;
    private LocalDateTime createdAt;
    private String introducer;
    private LocalDateTime updatedDate;
    private String logisticsNo;
    private LocalDateTime deliveryDate;
    private String payNo;
    private String payType;
    private String remark;
}