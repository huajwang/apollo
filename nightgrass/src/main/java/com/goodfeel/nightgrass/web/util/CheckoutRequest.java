package com.goodfeel.nightgrass.web.util;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CheckoutRequest {
    private Long orderId;
    public BigDecimal amount;
}
