package com.goodfeel.nightgrass.service;

import com.goodfeel.nightgrass.dto.OrderDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface IOrderService {

    Mono<Void> buildOrder(Long userId, String orderDetail, BigDecimal amount, String deliveryAddress,
                    String remark, Long introducer, Long cartId);

    Flux<OrderDto> getOrderListByUserId(Long userId);
}
