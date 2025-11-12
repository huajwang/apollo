package com.goodfeel.nightgrass.util

import com.goodfeel.nightgrass.serviceImpl.JwtService
import com.goodfeel.nightgrass.web.util.Utility
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Duration

/**
 * Once user is successfully authenticated with OAuth2 provider,
 * generate a JWT token and redirecting to the Angular frontend with the token
 * as a query parameter.
 */
class AuthenticationSuccessHandler(
    private val jwtService: JwtService
): ServerAuthenticationSuccessHandler {

    private val logger = LoggerFactory.getLogger(AuthenticationSuccessHandler::class.java)

    override fun onAuthenticationSuccess(
        webFilterExchange: WebFilterExchange,
        authentication: Authentication
    ): Mono<Void> {
        val tokenPair = jwtService.generateTokenPair(authentication)
        logger.debug("Generated JWT token pair for authenticated user: {}", tokenPair)
        // Redirect to Angular frontend with token
        val response = webFilterExchange.exchange.response
        // Store refresh token in secure HttpOnly cookie
        val refreshTokenCookie = ResponseCookie.from("refreshToken", tokenPair.refreshToken)
            .httpOnly(true)
            .secure(true) // TODO Set to true in production Utility.isProduction()
            .path("/")
            .maxAge(Duration.ofDays(7))
            .sameSite("Lax") // TODO - Consider "Strict" based on frontend requirements
            .build()
        response.addCookie(refreshTokenCookie)

        response.statusCode = HttpStatus.FOUND
        response.headers.location = URI.create(Utility.FRONTEND_URL + "/auth/callback?token=$${tokenPair.accessToken}")
        return response.setComplete()
    }
}
