package com.goodfeel.nightgrass.rest

import com.goodfeel.nightgrass.dto.CartItemDto
import com.goodfeel.nightgrass.serviceImpl.CartService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class CartUpdateRequest(val items: List<CartItemDto>)

@RestController
@RequestMapping("/api/cart")
class CartApiController(private val cartService: CartService) {

    private val logger = LoggerFactory.getLogger(CartApiController::class.java)

    @PostMapping("/update")
    fun updateCart(@RequestBody cartUpdateRequest: CartUpdateRequest ) {

    }

    @GetMapping("/my-cart")
    fun myCart() {
        // Implementation for retrieving the user's cart

    }

}
