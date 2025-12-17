package com.goodfeel.nightgrass.rest

import com.goodfeel.nightgrass.data.User
import com.goodfeel.nightgrass.data.Address
import com.goodfeel.nightgrass.repo.AddressRepository
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
    private val authenticationUtility: AuthenticationUtility,
    private val addressRepository: AddressRepository
) {
    private val logger = LoggerFactory.getLogger(UserApiController::class.java)

    @GetMapping("/profile")
    fun getUserProfile(exchange: ServerWebExchange): Mono<ResponseEntity<UserProfileDto>> {
        return authenticationUtility.extractUserFromExchange(exchange)
            .flatMap { user ->
                userService.getUserByOauthId(user.oauthId!!)
                    .flatMap { dbUser ->
                        addressRepository.findByUserIdAndIsDefaultTrue(dbUser.id!!)
                            .map { address ->
                                UserProfileDto(
                                    id = dbUser.id,
                                    oauthId = dbUser.oauthId,
                                    name = dbUser.nickName ?: dbUser.email ?: "User",
                                    nickName = dbUser.nickName,
                                    email = dbUser.email,
                                    avatar = dbUser.avatar,
                                    provider = dbUser.provider,
                                    customerName = address.customerName,
                                    phone = address.phone,
                                    address = address.addressLine,
                                    city = address.city,
                                    postalCode = address.postalCode
                                )
                            }
                            .defaultIfEmpty(
                                UserProfileDto(
                                    id = dbUser.id,
                                    oauthId = dbUser.oauthId,
                                    name = dbUser.nickName ?: dbUser.email ?: "User",
                                    nickName = dbUser.nickName,
                                    email = dbUser.email,
                                    avatar = dbUser.avatar,
                                    provider = dbUser.provider,
                                    customerName = null,
                                    phone = null,
                                    address = null,
                                    city = null,
                                    postalCode = null
                                )
                            )
                    }
                    .map { ResponseEntity.ok(it) }
            }
            .defaultIfEmpty(ResponseEntity.notFound().build())
    }

    @PostMapping("/address")
    fun updateAddress(
        @RequestBody request: UpdateAddressRequest,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Address>> {
        return authenticationUtility.extractUserFromExchange(exchange)
            .flatMap { user ->
                userService.updateAddress(
                    oauthId = user.oauthId!!,
                    customerName = request.customerName,
                    phone = request.phone,
                    address = request.address,
                    city = request.city,
                    postalCode = request.postalCode
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
    val postalCode: String
)

data class UserProfileDto(
    val id: Long?,
    val oauthId: String?,
    val name: String?,
    val nickName: String?,
    val email: String?,
    val avatar: String?,
    val provider: String?,
    val customerName: String?,
    val phone: String?,
    val address: String?,
    val city: String?,
    val postalCode: String?
)
