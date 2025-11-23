package com.goodfeel.nightgrass.util

import com.goodfeel.nightgrass.serviceImpl.JwtService
import com.goodfeel.nightgrass.data.User
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Utility component for extracting and validating authenticated users from HTTP requests.
 * Provides a centralized, reusable method for token extraction and JWT validation.
 *
 * OPTIMIZATION: Constructs User objects directly from JWT claims without database lookups.
 * This avoids N+1 database queries on every request. JWT claims should contain user identity info.
 * Designed to be injected into multiple controllers for consistent auth handling.
 */
@Component
class AuthenticationUtility(
    private val jwtService: JwtService
) {

    private val logger = LoggerFactory.getLogger(AuthenticationUtility::class.java)

    /**
     * Extracts token from request header, validates it, and constructs User from JWT claims.
     * Does NOT perform database lookups - User object is created from token claims only.
     * This ensures consistent performance: O(1) per request, no database round trips.
     *
     * @param exchange ServerWebExchange containing the HTTP request
     * @return Mono<User> with the authenticated user or empty if token is invalid
     */
    fun extractUserFromExchange(exchange: ServerWebExchange): Mono<User> {
        return extractTokenFromRequest(exchange)
            .doOnNext { logger.debug("Extracted token from request") }
            .flatMap { token ->
                jwtService.validateToken(token)
                    .doOnNext { logger.debug("Token validation successful") }
                    .flatMap { jwt ->
                        jwtService.getUserInfoFromToken(jwt)
                            .map { userInfo ->
                                logger.debug("Retrieved user info from token, OAuth ID: {}", userInfo.id)
                                // Construct User directly from JWT claims - no DB lookup needed
                                User(
                                    oauthId = userInfo.id,
                                    nickName = userInfo.name,
                                    email = userInfo.email,
                                    avatar = userInfo.avatar
                                )
                            }
                    }
            }
            .switchIfEmpty(
                Mono.error(Exception("Unable to extract token from request header"))
            )
    }

    /**
     * Extracts Bearer token from Authorization header.
     *
     * @param exchange ServerWebExchange containing the HTTP request
     * @return Mono<String> with the token or empty if not present/invalid
     */
    fun extractTokenFromRequest(exchange: ServerWebExchange): Mono<String> {
        val token = exchange.request.headers.getFirst("Authorization")
        logger.debug("Authorization header token present: {}", token != null)
        if (token != null) {
            return token.takeIf { it.startsWith("Bearer ") }
                ?.substringAfter("Bearer ")
                ?.let { Mono.just(it) }
                ?: Mono.empty()
        }
        return Mono.empty()
    }

    /**
     * Extracts and validates the token, then returns the OAuth ID from JWT claims.
     * Used to identify the user for database lookups.
     *
     * @param exchange ServerWebExchange containing the HTTP request
     * @return Mono<String> with the OAuth ID or empty if token is invalid
     */
    fun extractOAuthIdFromExchange(exchange: ServerWebExchange): Mono<String> {
        return extractTokenFromRequest(exchange)
            .flatMap { token ->
                jwtService.validateToken(token)
                    .flatMap { jwt ->
                        jwtService.getUserInfoFromToken(jwt)
                            .map { userInfo ->
                                logger.debug("Extracted OAuth ID from token: {}", userInfo.id)
                                userInfo.id
                            }
                    }
            }
    }
}
