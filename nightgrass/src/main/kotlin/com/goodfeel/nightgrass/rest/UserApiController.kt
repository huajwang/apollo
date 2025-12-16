package com.goodfeel.nightgrass.rest

import com.goodfeel.nightgrass.data.User
import com.goodfeel.nightgrass.service.UserService
import com.goodfeel.nightgrass.util.AuthenticationUtility
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/user")
class UserApiController(
    private val userService: UserService,
    private val authenticationUtility: AuthenticationUtility
) {
    private val logger = LoggerFactory.getLogger(UserApiController::class.java)

    @GetMapping("/profile")
    fun getUserProfile(exchange: ServerWebExchange): Mono<ResponseEntity<User>> {
        return authenticationUtility.extractUserFromExchange(exchange)
            .flatMap { user ->
                userService.getUserByOauthId(user.oauthId!!)
                    .map { ResponseEntity.ok(it) }
            }
            .defaultIfEmpty(ResponseEntity.notFound().build())
    }

    @PostMapping("/address")
    fun updateAddress(
        @RequestBody request: UpdateAddressRequest,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<User>> {
        return authenticationUtility.extractUserFromExchange(exchange)
            .flatMap { user ->
                userService.updateAddress(
                    oauthId = user.oauthId!!,
                    customerName = request.customerName,
                    phone = request.phone,
                    address = request.address,
                    city = request.city,
                    postalCode = request.postalCode,
                    email = request.email
                ).map { ResponseEntity.ok(it) }
            }
            .defaultIfEmpty(ResponseEntity.notFound().build())
    }
}

data class UpdateAddressRequest(
    val customerName: String,
    val phone: String,
    val address: String,
    val city: String,
    val postalCode: String,
    val email: String? = null
)
