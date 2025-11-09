package com.goodfeel.nightgrass.rest.auth

import com.goodfeel.nightgrass.serviceImpl.JwtService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
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

data class ValidationResponse(val valid: Boolean)

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

    @GetMapping("/verify")
    fun verifyToken(exchange: ServerWebExchange): Mono<ResponseEntity<ValidationResponse>> {
        return extractTokenFromRequest(exchange)
            .flatMap { token ->
                jwtService.validateToken(token)
                    .map {
                        ResponseEntity.ok(ValidationResponse(valid = true))
                    }
            }.switchIfEmpty(Mono.just(ResponseEntity.status(401).build()))
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