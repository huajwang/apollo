package com.goodfeel.nightgrass.repo;

import com.goodfeel.nightgrass.data.OrderItem;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface OrderItemRepository extends ReactiveCrudRepository<OrderItem, Long> {
}
