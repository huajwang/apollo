package com.goodfeel.nightgrass.repo

import com.goodfeel.nightgrass.data.Product
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface ProductRepository : ReactiveCrudRepository<Product, Long> {
}
