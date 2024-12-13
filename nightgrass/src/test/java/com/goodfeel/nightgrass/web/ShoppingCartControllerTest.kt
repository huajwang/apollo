package com.goodfeel.nightgrass.web

import com.goodfeel.nightgrass.data.*
import com.goodfeel.nightgrass.dto.CartItemDto
import com.goodfeel.nightgrass.serviceImpl.CartService
import com.goodfeel.nightgrass.serviceImpl.GuestService
import com.goodfeel.nightgrass.web.util.AddCartRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.http.server.reactive.MockServerHttpResponse
import org.springframework.ui.Model
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.math.BigDecimal
import java.security.Principal

class CartControllerTest {

    private lateinit var guestService: GuestService
    private lateinit var cartService: CartService
    private lateinit var cartController: ShoppingCartController
    private lateinit var model: Model

    @BeforeEach
    fun setup() {
        guestService = mock(GuestService::class.java)
        cartService = mock(CartService::class.java)
        model = mock(Model::class.java)
        cartController = ShoppingCartController(cartService, guestService)
    }

    @Test
    fun `viewCart should return shopping-cart view with cart items and total price`() {
        val principal = mock(Principal::class.java)
        val request = MockServerHttpRequest.get("/view").build()
        val response = MockServerHttpResponse()
        val cartId = 1L
        val user = User(oauthId = "user123")
        val cart = Cart(cartId = cartId, userId = user.oauthId, total = BigDecimal.valueOf(50))

        val cartItems = listOf(
            CartItemDto(productId = 1L, quantity = 2, productName = "e88",
                description = "goody", imageUrl = "", price = BigDecimal.valueOf(10),
                properties = ""),
            CartItemDto(productId = 2L, quantity = 1, productName = "e99",
                description = "not bad", imageUrl = "", price = BigDecimal.valueOf(30),
                properties = "")
        )
        val totalPrice = BigDecimal.valueOf(50)

        `when`(guestService.retrieveUserGuestOrCreate(principal, request, response)).thenReturn(Mono.just(user))
        `when`(cartService.getCartForUserOrGuest(user)).thenReturn(Mono.just(cart))
        `when`(cartService.getCartItemsForCart(cart.cartId!!)).thenReturn(Flux.fromIterable(cartItems))
        `when`(cartService.getTotalPriceForCart(cartId)).thenReturn(Mono.just(totalPrice))

        val result = cartController.viewCart(model, principal, request, response)

        StepVerifier.create(result)
            .expectNextMatches { viewName ->
                verify(model).addAttribute("cartItems", cartItems)
                verify(model).addAttribute("cartTotal", totalPrice)
                viewName == "shopping-cart"
            }
            .verifyComplete()

        verify(guestService).retrieveUserGuestOrCreate(principal, request, response)
        // verify(cartService).getCartForUserOrGuest(user)
        verify(cartService).getCartItemsForCart(cartId)
        verify(cartService).getTotalPriceForCart(cartId)
    }

    @Test
    fun `viewCart should return error view when an exception occurs`() {
        val principal = mock(Principal::class.java)
        val user = User(oauthId = principal.name)
        val request = MockServerHttpRequest.get("/view").build()
        val response = MockServerHttpResponse()

        `when`(guestService.retrieveUserGuestOrCreate(principal, request, response))
            .thenReturn(Mono.error(RuntimeException("User retrieval failed")))

        val result = cartController.viewCart(model, principal, request, response)

        StepVerifier.create(result)
            .expectNextMatches { viewName ->
                verify(model).addAttribute("error", "Failed to load cart")
                viewName == "error"
            }
            .verifyComplete()

        verify(guestService).retrieveUserGuestOrCreate(principal, request, response)
        verify(cartService, never()).getCartForUserOrGuest(user)
        verify(cartService, never()).getCartItemsForCart(anyLong())
        verify(cartService, never()).getTotalPriceForCart(anyLong())
    }

    @Test
    fun `addToCart should add product to cart and return updated cart item count`() {
        val addCartRequest = AddCartRequest(productId = 1L, properties = mutableMapOf("color" to "red"))
        val userId = "user123"
        val user = User(oauthId = userId)
        val principal = mock(Principal::class.java)
        val request = MockServerHttpRequest.post("/add").build()
        val response = MockServerHttpResponse()
        val cart = Cart(cartId = 1L, total = BigDecimal.valueOf(99.9), userId = userId, guestId = null)

        `when`(guestService.retrieveUserGuestOrCreate(principal, request, response)).thenReturn(Mono.just(user))
        `when`(cartService.addProductToCart(addCartRequest, user)).thenReturn(Mono.just(cart))
        `when`(cartService.getCartItemCount(user)).thenReturn(Mono.just(5))

        val result = cartController.addToCart(addCartRequest, principal, request, response)

        StepVerifier.create(result)
            .expectNextMatches { responseEntity ->
                responseEntity.statusCode.is2xxSuccessful &&
                        responseEntity.body?.get("cartItemCount") == 5
            }
            .verifyComplete()

        verify(guestService).retrieveUserGuestOrCreate(principal, request, response)
        verify(cartService).addProductToCart(addCartRequest, user)
        verify(cartService).getCartItemCount(user)
    }

    @Test
    fun `addToCart should handle error and return bad request`() {
        val addCartRequest = AddCartRequest(productId = 1L, properties = mutableMapOf("color" to "red"))
        val principal = mock(Principal::class.java)
        val user = User(oauthId = principal.name)
        val request = MockServerHttpRequest.post("/add").build()
        val response = MockServerHttpResponse()

        `when`(guestService.retrieveUserGuestOrCreate(principal, request, response))
            .thenReturn(Mono.error(RuntimeException("User retrieval failed")))

        val result = cartController.addToCart(addCartRequest, principal, request, response)

        StepVerifier.create(result)
            .expectNextMatches { responseEntity ->
                responseEntity.statusCode.is4xxClientError &&
                        responseEntity.body?.get("error") == "Failed to add product to cart"
            }
            .verifyComplete()

        verify(guestService).retrieveUserGuestOrCreate(principal, request, response)
        verify(cartService, never()).addProductToCart(addCartRequest, user)
        verify(cartService, never()).getCartItemCount(user)
    }

}
