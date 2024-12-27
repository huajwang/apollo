package com.goodfeel.nightgrass.repo

import com.goodfeel.nightgrass.data.Product
import com.goodfeel.nightgrass.dto.ProductDto
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface ProductRepository : ReactiveCrudRepository<Product, Long> {

    @Query("SELECT p.product_id, p.product_name, p.description, p.image_url, p.price, " +
            "p.category, d.discount_type, d.discount_value\n" +
            "FROM e_mall_product p\n" +
            "LEFT JOIN e_mall_discount d\n" +
            "ON p.product_id = d.product_id\n" +
            "WHERE p.category = 'BIG_HIT'\n" +
            "LIMIT 3;")
    fun findTop3BigHits(): Flux<ProductDto>

    @Query("SELECT p.product_id, p.product_name, p.description, p.image_url, p.price, " +
            "p.category, d.discount_type, d.discount_value\n" +
            "FROM e_mall_product p\n" +
            "LEFT JOIN e_mall_discount d\n" +
            "ON p.product_id = d.product_id\n" +
            "WHERE p.category = 'POPULAR' || p.category = 'NEW'\n" +
            "LIMIT 8;")
    fun findTop8PopularOrNewProducts(): Flux<ProductDto>

    @Query("SELECT p.product_id, p.product_name, p.description, p.image_url, p.price, " +
        "p.additional_info, p.category, d.discount_type, d.discount_value\n" +
        "FROM e_mall_product p\n" +
        "LEFT JOIN e_mall_discount d\n" +
        "ON p.product_id = d.product_id\n" +
        "WHERE p.product_id = :id")
    fun findByProductId(id: Long): Mono<ProductDto>

    @Query("SELECT p.product_id, p.product_name, p.description, p.image_url, p.price, " +
        "p.additional_info, p.category, d.discount_type, d.discount_value\n" +
        "FROM e_mall_product p\n" +
        "LEFT JOIN e_mall_discount d\n" +
        "ON p.product_id = d.product_id"
    )
    fun findAllProducts(): Flux<ProductDto>
}
