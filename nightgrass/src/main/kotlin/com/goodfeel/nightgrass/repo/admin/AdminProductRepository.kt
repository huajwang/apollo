package com.goodfeel.nightgrass.repo.admin

import com.goodfeel.nightgrass.data.admin.AdminProduct
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface AdminProductRepository: ReactiveCrudRepository<AdminProduct, Long> {
}