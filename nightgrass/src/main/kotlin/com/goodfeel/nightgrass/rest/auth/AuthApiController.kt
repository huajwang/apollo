package com.goodfeel.nightgrass.rest.auth

import com.goodfeel.nightgrass.serviceImpl.JwtService
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
class AuthApiController(private val jwtService: JwtService) {

    private val logger = LoggerFactory.getLogger(AuthApiController::class.java)

    @GetMapping("/user")
    fun getCurrentUser(exchange: ServerWebExchange): Mono<ResponseEntity<UserInfo>> {
        return extractTokenFromRequest(exchange)
            .doOnNext { logger.debug("Extracted token from request: $it") }
            .flatMap { token ->
                jwtService.validateToken(token).doOnNext { logger.debug("Token validation result: {}", it) }
                    .flatMap { jwt ->
                        jwtService.getUserInfoFromToken(jwt)
                            .map { userInfo ->
                                logger.debug("Retrieved user info from token: {}", userInfo)
                                ResponseEntity.ok(userInfo)
                            }
                    }
                    .switchIfEmpty(Mono.just(ResponseEntity.status(401).build()))
            }.switchIfEmpty(Mono.just(ResponseEntity.status(401).build()))
    }

    @PostMapping("/refresh")
    fun refreshToken(@RequestBody request: RefreshTokenRequest): Mono<ResponseEntity<TokenResponse>> {
        return jwtService.refreshTokenPair(refreshToken = request.refreshToken)
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

    private fun extractTokenFromRequest(exchange: ServerWebExchange): Mono<String> {
        val token = exchange.request.headers.getFirst("Authorization")
        logger.debug("Authorization header token: {}", token)
        if (token != null) {
            return token.takeIf { it.startsWith("Bearer ") }
                ?.substringAfter("Bearer ")
                ?.let { Mono.just(it) }
                ?: Mono.empty()
        }
        return Mono.empty()
    }
}