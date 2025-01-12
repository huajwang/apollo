package com.goodfeel.nightgrass.rest

import com.goodfeel.nightgrass.data.Product
import com.goodfeel.nightgrass.serviceImpl.ProductService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/search")
class SearchController(
    private val productService: ProductService
) {
    @RequestMapping
    fun searchProduct(@RequestParam query: String): Flux<Product> {
        return productService.findByNameContainingIgnoreCase(query)
    }
}