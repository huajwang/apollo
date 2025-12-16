package com.goodfeel.nightgrass.serviceImpl

import com.goodfeel.nightgrass.rest.auth.UserInfo
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.oauth2.jwt.*
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.*

@Service
class JwtService(
    private val jwtEncoder: JwtEncoder,
    private val jwtDecoder: ReactiveJwtDecoder
) {

    private val accessTokenExpirySecond = 15 * 60L // 15 minutes
    private val refreshTokenExpirySecond = 7 * 24 * 60 * 60L // 7 days

    data class TokenPair(
        val accessToken: String,
        val refreshToken: String,
        val expiresIn: Long // in seconds
    )

    fun generateTokenPair(authentication: Authentication): TokenPair {
        val oauth2User = authentication.principal as OAuth2User
        val userId = getUserId(oauth2User)
        val userName = getUserName(oauth2User)
        val userEmail = getUserEmail(oauth2User)
        val userAvatarUrl = getUserAvatarUrl(oauth2User)
        
        val provider = if (authentication is OAuth2AuthenticationToken) {
            authentication.authorizedClientRegistrationId
        } else {
            authentication.name
        }

        val accessToken = generateAccessToken(userId, userName, userEmail, userAvatarUrl, provider)
        val refreshToken = generateRefreshToken(userId) // In real scenario, store this in DB or cache TODO
        return TokenPair(accessToken, refreshToken, accessTokenExpirySecond)
    }

    private fun generateAccessToken(
        userId: String,
        userName: String,
        userEmail: String?,
        userAvatarUrl: String,
        provider: String): String {
        val now = Instant.now()
        val expiryDate = now.plusSeconds(accessTokenExpirySecond)
        
        val claimsBuilder = JwtClaimsSet.builder()
            .issuer("Yaojia Buy")
            .issuedAt(now)
            .expiresAt(expiryDate)
            .subject(userId)
            .claim("name", userName)
            .claim("avatarUrl", userAvatarUrl)
            .claim("provider", provider)
            .claim("type", "access")
            .claim("scope", "read write")

        if (userEmail != null) {
            claimsBuilder.claim("email", userEmail)
        }

        return jwtEncoder.encode(
            JwtEncoderParameters.from(
                claimsBuilder.build()
            )
        ).tokenValue
    }

    private fun generateRefreshToken(userId: String): String {
        val now = Instant.now()
        val expiryDate = now.plusSeconds(refreshTokenExpirySecond)
        val tokenId = UUID.randomUUID().toString()
        return jwtEncoder.encode(
            JwtEncoderParameters.from(
                JwtClaimsSet.builder()
                    .issuer("Yaojia Buy")
                    .issuedAt(now)
                    .expiresAt(expiryDate)
                    .subject(userId)
                    .claim("type", "refresh")
                    .claim("jti", tokenId)
                    .build()
            )
        ).tokenValue
    }

    private fun validateRefreshToken(refreshToken: String): Mono<Jwt> {
        return jwtDecoder.decode(refreshToken)
            .filter { jwt -> 
                val isRefresh = jwt.claims["type"] == "refresh"
                if (!isRefresh) println("Token validation failed: type is ${jwt.claims["type"]}")
                isRefresh
            }
            .doOnError { e -> println("Token validation error: ${e.message}") }
            .onErrorResume { Mono.empty() } // Invalid or expired token
    }

    fun refreshTokenPair(refreshToken: String): Mono<TokenPair> {
        return validateRefreshToken(refreshToken)
            .flatMap { jwt ->
                val userId = jwt.subject
                // TODO: In real scenario, verify the jti against DB or cache to ensure it's valid and not revoked
                // TODO: Consider fetch from database
                val userName = jwt.claims["name"] as? String ?: ""
                val userEmail = jwt.claims["email"] as? String ?: ""
                val userAvatarUrl = jwt.claims["avatarUrl"] as? String ?: ""
                val provider = jwt.claims["provider"] as? String ?: ""

                val newAccessToken = generateAccessToken(userId, userName, userEmail, userAvatarUrl, provider)
                val newRefreshToken = generateRefreshToken(userId) // In real scenario, update this in DB or cache TODO

                Mono.just(TokenPair(newAccessToken, newRefreshToken, accessTokenExpirySecond))
            }
    }

    private fun getUserId(oauth2User: OAuth2User): String {
        val attributes = oauth2User.attributes
        return  when {
            attributes.containsKey("sub") -> attributes["sub"].toString() // Google
            attributes.containsKey("open_id") -> attributes["open_id"].toString() // TikTok
            attributes.containsKey("openid") -> attributes["openid"].toString() // WeChat
            attributes.containsKey("login") -> attributes["login"].toString() // GitHub (use login as unique ID for consistency with DB)
            attributes.containsKey("id") -> attributes["id"].toString() // Facebook, GitHub (numeric ID)
            // general fallback for other providers
            attributes.containsKey("user_id") -> attributes["user_id"].toString()
            // use oauth2User.name as last resort
            else -> oauth2User.name ?: UUID.randomUUID().toString()
        }
    }

    private fun getUserName(oauth2User: OAuth2User): String {
        val attributes = oauth2User.attributes
        return when {
            attributes.containsKey("name") && attributes["name"] != null -> attributes["name"].toString() // General name field
            attributes.containsKey("login") -> attributes["login"].toString() // GitHub username
            attributes.containsKey("nickname") -> attributes["nickname"].toString() // TikTok
            attributes.containsKey("given_name") && attributes.containsKey("family_name") ->
                "${attributes["given_name"].toString()} ${attributes["family_name"].toString()}" // Google
            attributes.containsKey("displayName") -> attributes["displayName"].toString() // Facebook
            attributes.containsKey("username") -> attributes["username"].toString() // General username field
            // Fallback to email prefix if name not available
            attributes.containsKey("email") && attributes["email"] != null -> {
                val email = attributes["email"].toString()
                email.substringBefore("@")
            }
            else -> "User" // Default fallback
        }
    }

    private fun getUserEmail(oauth2User: OAuth2User): String? {
        val attributes = oauth2User.attributes
        return when {
            attributes.containsKey("email") && attributes["email"] != null -> attributes["email"].toString()
            attributes.containsKey("email_address") && attributes["email_address"] != null -> attributes["email_address"].toString()
            else -> null // Email not available
        }
    }

    private fun getUserAvatarUrl(oauth2User: OAuth2User): String {
        val attributes = oauth2User.attributes
        return when {
            attributes.containsKey("picture") -> attributes["picture"].toString() // Google, Facebook
            attributes.containsKey("avatar_url") -> attributes["avatar_url"].toString() // TikTok
            attributes.containsKey("headimgurl") -> attributes["headimgurl"].toString() // WeChat
            else -> "" // Avatar URL not available
        }
    }

    fun validateToken(token: String): Mono<Jwt> = jwtDecoder.decode(token)

    fun getUserInfoFromToken(jwt: Jwt): Mono<UserInfo> {
        val userInfo = UserInfo(
            id = jwt.subject,
            name = jwt.claims["name"] as String,
            email = jwt.claims["email"] as? String,
            avatar = jwt.claims["avatarUrl"] as? String,
            provider = jwt.claims["provider"] as? String
        )
        return Mono.just(userInfo)
    }

}
