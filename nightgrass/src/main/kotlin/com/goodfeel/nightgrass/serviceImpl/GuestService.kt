package com.goodfeel.nightgrass.serviceImpl

import com.goodfeel.nightgrass.data.User
import com.goodfeel.nightgrass.repo.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.security.Principal
import java.util.*

@Service
class GuestService(
    private val jwtService: JwtService,
    private val userRepository: UserRepository
) {

    private val logger = LoggerFactory.getLogger(GuestService::class.java)

    /**
     * If user already logged-in. return logged-in user;
     * If the current customer is Guest, find the guestId from cookie or HTTP header.
     * For the first request, there is no guestId in cookie or header, generate a guestId, respond it to
     * client and save the Guest user in DB.
     * The subsequent requests after the first request will see the guestId in cookie or header and find
     * the Guest user from DB.
     */
    fun retrieveUserGuestOrCreate(
        principal: Principal?,
        request: ServerHttpRequest,
        response: ServerHttpResponse): Mono<User> {
        val userId = principal?.name
        if (userId != null) {
            return userRepository.findByOauthId(userId)
        }
        return getOrCreateGuestId(request, response).flatMap { guestId ->
            userRepository.findByGuestId(guestId)
                .switchIfEmpty(
                    Mono.defer {
                        userRepository.save(User(guestId = guestId))
                            .doOnSuccess {
                                logger.debug("getOrCreateGuestId. Saved Guest user guestId: $guestId")
                            }
                    }

                )
        }
    }

    /**
     * Purely get the guestId from cookie or header
     * if no guestId presented in cookie or header, return Mono.empty()
     * This one is mainly for CartMergeAuthenticationSuccessHandler to merge
     * Guest shopping cart and orders. If there is no Guest at all, just simply
     * skip the merge process.
     *
     * E.g. If customer does not add product to cart, does not view shopping cart, then
     * no guestId should be added into cookie/header. And no entry should be saved into
     * DB as Guest. TODO - to confirm this is true
     */
    fun getGuestId(request: ServerHttpRequest): Mono<String> {
        val guestIdCookie = request.cookies["guestId"]?.firstOrNull()
        if (guestIdCookie != null) {
            return Mono.just(guestIdCookie.value)
        }
        // Check for guestId in Authorization header (JWT token)
        val authToken = request.headers.getFirst(HttpHeaders.AUTHORIZATION)?.removePrefix("Bearer ")
        if (authToken != null) {
            return jwtService.validateAndExtractGuestId(authToken)
        }
        return Mono.empty()
    }

    /**
     * If logged-in user present, return logged-in user;
     * If Guest presents, return Guest.
     * A Guest is not created in this function.
     */
    fun retrieveUserOrGuest(
        principal: Principal?,
        request: ServerHttpRequest,
    ): Mono<User> {
        val userId = principal?.name
        if (userId != null) {
            return userRepository.findByOauthId(userId)
        }
        return getGuestId(request).flatMap {
            userRepository.findByGuestId(it)
        }
    }

    private fun getOrCreateGuestId(
        request: ServerHttpRequest,
        response: ServerHttpResponse
    ): Mono<String> {
        // Check for guestId in cookies
        val guestIdCookie = request.cookies["guestId"]?.firstOrNull()
        if (guestIdCookie != null) {
            return Mono.just(guestIdCookie.value)
        }

        // Check for guestId in Authorization header (JWT token)
        val authToken = request.headers.getFirst(HttpHeaders.AUTHORIZATION)?.removePrefix("Bearer ")
        if (authToken != null) {
            return jwtService.validateAndExtractGuestId(authToken)
                .switchIfEmpty(generateNewGuestId(response)) // Generate a new guestId if token is invalid
        }

        // Generate a new guestId for the first request and add it in response cookie and JWT token
        return generateNewGuestId(response)
    }

    private fun generateNewGuestId(response: ServerHttpResponse): Mono<String> {
        val newGuestId = UUID.randomUUID().toString()

        // Set the guestId in a secure cookie
        response.addCookie(
            ResponseCookie.from("guestId", newGuestId)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .build()
        )

        // Generate a new JWT for the guestId
        val jwtToken = jwtService.generateJwt(newGuestId)
        response.headers.add(HttpHeaders.AUTHORIZATION, "Bearer $jwtToken")

        return Mono.just(newGuestId)
    }
}
