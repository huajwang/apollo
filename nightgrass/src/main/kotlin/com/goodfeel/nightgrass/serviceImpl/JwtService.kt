package com.goodfeel.nightgrass.serviceImpl

import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant

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
}
