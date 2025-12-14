package com.goodfeel.nightgrass.rest.auth

import com.goodfeel.nightgrass.serviceImpl.JwtService
import com.goodfeel.nightgrass.util.AuthenticationUtility
import com.goodfeel.nightgrass.repo.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

data class UserInfo(
    val id: String,
    val name: String,
    val email: String?,
    val avatar: String?,
    val provider: String?
)

data class RefreshTokenRequest(val refreshToken: String)
data class TokenResponse(val accessToken: String, val refreshToken: String, val expiresIn: Long)

@RestController
@RequestMapping("/api/auth")
class AuthApiController(
    private val jwtService: JwtService,
    private val authenticationUtility: AuthenticationUtility,
    private val userRepository: UserRepository
) {

    private val logger = LoggerFactory.getLogger(AuthApiController::class.java)

    @GetMapping("/user")
    fun getCurrentUser(exchange: ServerWebExchange): Mono<ResponseEntity<UserInfo>> {
        return authenticationUtility.extractOAuthIdFromExchange(exchange)
            .flatMap { oauthId ->
                userRepository.findByOauthId(oauthId)
                    .map { user ->
                        logger.debug("Retrieved user from database: {}", user.id)
                        ResponseEntity.ok(
                            UserInfo(
                                id = user.oauthId ?: "",
                                name = user.nickName ?: "",
                                email = user.email,
                                avatar = user.avatar,
                                provider = user.provider
                            )
                        )
                    }
            }
            .switchIfEmpty(Mono.just(ResponseEntity.status(401).build()))
            .onErrorResume {
                logger.debug("Failed to retrieve current user: {}", it.message)
                Mono.just(ResponseEntity.status(401).build())
            }
    }

    @PostMapping("/refresh")
    fun refreshToken(
        @RequestBody request: RefreshTokenRequest,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<TokenResponse>> {
        // Get refresh token from request body or from HttpOnly cookie
        val cookieToken = exchange.request.cookies.getFirst("refreshToken")?.value
        val origin = exchange.request.headers.getFirst("Origin")
        val referer = exchange.request.headers.getFirst("Referer")
        logger.info("RefreshToken request received. Origin: {}, Referer: {}, Body token present: {}, Cookie token present: {}", origin, referer, request.refreshToken.isNotEmpty(), cookieToken != null)
        
        val refreshToken = if (request.refreshToken.isNotEmpty()) {
            request.refreshToken
        } else {
            cookieToken ?: ""
        }
        
        return if (refreshToken.isEmpty()) {
            logger.warn("No refresh token found in request body or cookies")
            Mono.just(ResponseEntity.status(401).build())
        } else {
            jwtService.refreshTokenPair(refreshToken = refreshToken)
                .map { tokenPair ->
                    logger.debug("Generated new token pair from refresh token")
                    ResponseEntity.ok(
                        TokenResponse(
                            accessToken = tokenPair.accessToken,
                            refreshToken = tokenPair.refreshToken,
                            expiresIn = tokenPair.expiresIn
                        )
                    )
                }
                .switchIfEmpty(Mono.just(ResponseEntity.status(401).build()))
        }
    }
}