package com.goodfeel.nightgrass.util

import com.goodfeel.nightgrass.service.UserService
import com.goodfeel.nightgrass.serviceImpl.JwtService
import com.goodfeel.nightgrass.web.util.Utility
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import org.springframework.security.oauth2.core.user.OAuth2User
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Duration

/**
 * Once user is successfully authenticated with OAuth2 provider:
 * 1. Save or update user profile in database (name, email, avatar)
 * 2. Generate JWT token pair from user data
 * 3. Redirect to Angular frontend with access token as query parameter
 * 4. Store refresh token in secure HttpOnly cookie
 *
 * This ensures user data is persisted for future operations, orders, and user management.
 */
class AuthenticationSuccessHandler(
    private val jwtService: JwtService,
    private val userService: UserService
): ServerAuthenticationSuccessHandler {

    private val logger = LoggerFactory.getLogger(AuthenticationSuccessHandler::class.java)

    override fun onAuthenticationSuccess(
        webFilterExchange: WebFilterExchange,
        authentication: Authentication
    ): Mono<Void> {
        val oauth2User = authentication.principal as OAuth2User
        
        // Extract user info from OAuth2 provider
        val userId = extractUserId(oauth2User)
        val userName = extractUserName(oauth2User)
        val userEmail = extractUserEmail(oauth2User)
        val userAvatarUrl = extractUserAvatarUrl(oauth2User)
        
        logger.debug("OAuth2 authentication successful for user: $userId")
        
        // Save or update user in database
        return userService.saveOrUpdateOAuth2User(
            oauthId = userId,
            nickName = userName,
            email = userEmail,
            avatar = userAvatarUrl
        )
            .flatMap { savedUser ->
                logger.debug("User saved to database: ${savedUser.oauthId}")
                
                // Generate JWT token pair using saved user data
                val tokenPair = jwtService.generateTokenPair(authentication)
                logger.debug("Generated JWT token pair for authenticated user: {}", tokenPair)
                
                // Redirect to Angular frontend with token and set refresh token cookie
                val response = webFilterExchange.exchange.response
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
                response.setComplete()
            }
            .onErrorResume { error ->
                logger.error("Failed to save user or generate tokens during OAuth authentication", error)
                Mono.error(error)
            }
    }

    /**
     * Extract user ID from OAuth2 provider attributes.
     * Different providers use different claim names (sub, id, user_id, etc.)
     */
    private fun extractUserId(oauth2User: OAuth2User): String {
        return oauth2User.getAttribute("sub") as? String
            ?: oauth2User.getAttribute("id") as? String
            ?: oauth2User.getAttribute("user_id") as? String
            ?: oauth2User.name
    }

    private fun extractUserName(oauth2User: OAuth2User): String {
        return oauth2User.getAttribute("name") as? String
            ?: oauth2User.getAttribute("given_name") as? String
            ?: ""
    }

    private fun extractUserEmail(oauth2User: OAuth2User): String {
        return oauth2User.getAttribute("email") as? String ?: ""
    }

    private fun extractUserAvatarUrl(oauth2User: OAuth2User): String {
        return oauth2User.getAttribute("picture") as? String
            ?: oauth2User.getAttribute("avatar_url") as? String
            ?: ""
    }
}

