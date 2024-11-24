package com.goodfeel.nightgrass.repo

import com.goodfeel.nightgrass.data.Order
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface OrderRepository : ReactiveCrudRepository<Order, Long>
