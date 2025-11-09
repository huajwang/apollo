package com.goodfeel.nightgrass.serviceImpl

import com.goodfeel.nightgrass.rest.auth.UserInfo
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.oauth2.jwt.*
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.*

@Service
class JwtService(
    private val jwtEncoder: JwtEncoder,
    private val jwtDecoder: ReactiveJwtDecoder
) {

    // Generate a JWT token with the guestId claim
    fun generateJwt(guestId: String): String {
        val now = Instant.now()
        val claims = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(now.plusSeconds(3600)) // Token valid for 1 hour
            .claim("guestId", guestId)
            .build()

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).tokenValue
    }

    // Validate and extract the guestId from the JWT token
    fun validateAndExtractGuestId(token: String): Mono<String> {
        return jwtDecoder.decode(token)
            .map { jwt -> jwt.claims["guestId"] as String }
            .onErrorResume {
                // Handle invalid or expired token
                Mono.empty()
            }
    }

    fun generateToken(authentication: Authentication): String {
        val oauth2User = authentication.principal as OAuth2User
        val userId = getUserId(oauth2User)
        val userName = getUserName(oauth2User)
        val userEmail = getUserEmail(oauth2User)
        val userAvatarUrl = getUserAvatarUrl(oauth2User)
        val provider = authentication.name
        return generateToken(
            userId,
            userName,
            userEmail,
            userAvatarUrl,
            provider
        )
    }

    private fun generateToken(
        userId: String,
        userName: String,
        userEmail: String,
        userAvatarUrl: String,
        provider: String): String {
        val now = Date()
        val expiryDate = Date(now.time + 3600 * 1000) // 1 hour expiration
        return jwtEncoder.encode(
            JwtEncoderParameters.from(
                JwtClaimsSet.builder()
                    .issuer("Yaojia Buy")
                    .issuedAt(now.toInstant())
                    .expiresAt(expiryDate.toInstant())
                    .subject(userId)
                    .claim("name", userName)
                    .claim("email", userEmail)
                    .claim("avatarUrl", userAvatarUrl)
                    .claim("provider", provider)
                    .build()
            )
        ).tokenValue
    }

    private fun getUserId(oauth2User: OAuth2User): String {
        val attributes = oauth2User.attributes
        return  when {
            attributes.containsKey("sub") -> attributes["sub"].toString() // Google
            attributes.containsKey("open_id") -> attributes["open_id"].toString() // TikTok
            attributes.containsKey("openid") -> attributes["openid"].toString() // WeChat
            attributes.containsKey("id") -> attributes["id"].toString() // Facebook
            // general fallback for other providers
            attributes.containsKey("user_id") -> attributes["user_id"].toString()
            // use oauth2User.name as last resort
            else -> oauth2User.name ?: UUID.randomUUID().toString()
        }
    }

    private fun getUserName(oauth2User: OAuth2User): String {
        val attributes = oauth2User.attributes
        return when {
            attributes.containsKey("name") -> attributes["name"].toString() // General name field
            attributes.containsKey("nickname") -> attributes["nickname"].toString() // TikTok
            attributes.containsKey("given_name") && attributes.containsKey("family_name") ->
                "${attributes["given_name"].toString()} ${attributes["family_name"].toString()}" // Google
            attributes.containsKey("displayName") -> attributes["displayName"].toString() // Facebook
            attributes.containsKey("username") -> attributes["username"].toString() // General username field
            // Fallback to email prefix if name not available
            attributes.containsKey("email") -> {
                val email = attributes["email"].toString()
                email.substringBefore("@")
            }
            else -> "User" // Default fallback
        }
    }

    private fun getUserEmail(oauth2User: OAuth2User): String {
        val attributes = oauth2User.attributes
        return when {
            attributes.containsKey("email") -> attributes["email"].toString()
            attributes.containsKey("email_address") -> attributes["email_address"].toString()
            else -> "" // Email not available
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
