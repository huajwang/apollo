package com.goodfeel.nightgrass.web

import com.goodfeel.nightgrass.service.OrderService
import com.goodfeel.nightgrass.service.UserService
import com.goodfeel.nightgrass.serviceImpl.CartService
import com.goodfeel.nightgrass.serviceImpl.GuestService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.savedrequest.ServerRequestCache
import java.net.URI
import org.springframework.web.server.WebFilterChain


class MergeAuthenticationSuccessHandlerTest {

    private lateinit var cartService: CartService
    private lateinit var guestService: GuestService
    private lateinit var orderService: OrderService
    private lateinit var userService: UserService
    private lateinit var requestCache: ServerRequestCache
    private lateinit var successHandler: MergeAuthenticationSuccessHandler

    @BeforeEach
    fun setup() {
        cartService = mock(CartService::class.java)
        guestService = mock(GuestService::class.java)
        orderService = mock(OrderService::class.java)
        userService = mock(UserService::class.java)
        requestCache = mock(ServerRequestCache::class.java)

        successHandler = MergeAuthenticationSuccessHandler(
            cartService, guestService, orderService, userService, requestCache
        )
    }

    @Test
    fun `onAuthenticationSuccess should handle cart and order merge and redirect to target URL`() {
        val userId = "user123"
        val guestId = "guest123"
        val targetUri = URI.create("/dashboard")

        val authentication = mock(Authentication::class.java)
        `when`(authentication.name).thenReturn(userId)

        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/login"))
        val webFilterExchange = WebFilterExchange(exchange, mock(WebFilterChain::class.java))

        val request = exchange.request
        `when`(guestService.getGuestId(request)).thenReturn(Mono.just(guestId))
        `when`(cartService.mergeCart(userId, guestId)).thenReturn(Mono.empty())
        `when`(orderService.mergeOrder(userId, guestId)).thenReturn(Mono.empty())
        `when`(userService.deleteUserByGuestId(guestId)).thenReturn(Mono.empty())
        `when`(requestCache.getRedirectUri(exchange)).thenReturn(Mono.just(targetUri))

        val result = successHandler.onAuthenticationSuccess(webFilterExchange, authentication)

        StepVerifier.create(result)
            .verifyComplete()

        // Ensure mergeCart is invoked on user success authentication with userId and guestId
        verify(cartService).mergeCart(userId, guestId)
        verify(orderService).mergeOrder(userId, guestId)
        verify(userService).deleteUserByGuestId(guestId)
        verify(requestCache).getRedirectUri(exchange)

        assert(exchange.response.statusCode == HttpStatus.FOUND)
        assert(exchange.response.headers.location == targetUri)
    }

    @Test
    fun `onAuthenticationSuccess should not block login on merge errors`() {
        val userId = "user123"
        val guestId = "guest123"
        val targetUri = URI.create("/home")

        val authentication = mock(Authentication::class.java)
        `when`(authentication.name).thenReturn(userId)

        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/login"))
        val webFilterExchange = WebFilterExchange(exchange, mock(WebFilterChain::class.java))
        val request = exchange.request

        `when`(guestService.getGuestId(request)).thenReturn(Mono.just(guestId))
        `when`(cartService.mergeCart(userId, guestId)).thenReturn(Mono.error(RuntimeException("Cart merge error")))
        `when`(orderService.mergeOrder(userId, guestId)).thenReturn(Mono.error(RuntimeException("Order merge error")))
        `when`(userService.deleteUserByGuestId(guestId)).thenReturn(Mono.error(RuntimeException("Delete user error")))
        `when`(requestCache.getRedirectUri(exchange)).thenReturn(Mono.just(targetUri))


        val result = successHandler.onAuthenticationSuccess(webFilterExchange, authentication)

        StepVerifier.create(result)
            .verifyComplete()

        verify(cartService).mergeCart(userId, guestId)
        verify(orderService).mergeOrder(userId, guestId)
        verify(userService).deleteUserByGuestId(guestId)
        verify(requestCache).getRedirectUri(any())

        assert(exchange.response.statusCode == HttpStatus.FOUND)
        assert(exchange.response.headers.location == targetUri)
    }
}
