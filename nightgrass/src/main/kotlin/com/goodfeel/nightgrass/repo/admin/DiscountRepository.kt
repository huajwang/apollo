package com.goodfeel.nightgrass.repo.admin

import com.goodfeel.nightgrass.data.admin.Discount
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface DiscountRepository: ReactiveCrudRepository<Discount, Long> {
}