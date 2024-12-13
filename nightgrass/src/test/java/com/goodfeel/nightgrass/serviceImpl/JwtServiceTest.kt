package com.goodfeel.nightgrass.serviceImpl

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import reactor.core.publisher.Mono
import java.time.Instant

class JwtServiceTest {

    private val jwtEncoder: JwtEncoder = mock(JwtEncoder::class.java)
    private val jwtDecoder: ReactiveJwtDecoder = mock(ReactiveJwtDecoder::class.java)
    private val jwtService = JwtService(jwtEncoder, jwtDecoder)

    @Test
    fun `should generate JWT token successfully`() {
        // Arrange
        val guestId = "test-guest-id"
        val encodedToken = "mocked-jwt-token"
        // Mock the Jwt object
        val mockJwt = mock(Jwt::class.java)
        `when`(mockJwt.tokenValue).thenReturn(encodedToken)
        `when`(jwtEncoder.encode(any(JwtEncoderParameters::class.java))).thenReturn(mockJwt)

        // Act
        val token = jwtService.generateJwt(guestId)

        // Assert
        assertNotNull(token, "Generated token should not be null")
        assertEquals(encodedToken, token, "Token value should match the mocked value")
    }

    @Test
    fun `should validate JWT and extract guestId successfully`() {
        // Arrange
        val token = "mocked-jwt-token"
        val guestId = "test-guest-id"
        val now = Instant.now()
        val claims = mapOf(
            "guestId" to guestId,
            "iat" to now.epochSecond,
            "exp" to now.plusSeconds(3600).epochSecond
        )
        val jwt = Jwt(token, now, now.plusSeconds(3600), mapOf("alg" to "HS256"), claims)

        `when`(jwtDecoder.decode(token)).thenReturn(Mono.just(jwt))

        // Act
        val extractedGuestId = jwtService.validateAndExtractGuestId(token).block()

        // Assert
        assertNotNull(extractedGuestId, "Extracted guestId should not be null")
        assertEquals(guestId, extractedGuestId, "Extracted guestId should match the mocked value")
    }

    @Test
    fun `should handle invalid or expired JWT token`() {
        // Arrange
        val invalidToken = "invalid-jwt-token"
        `when`(jwtDecoder.decode(invalidToken)).thenReturn(Mono.error(RuntimeException("Invalid token")))

        // Act
        val extractedGuestId = jwtService.validateAndExtractGuestId(invalidToken).block()

        // Assert
        assertNull(extractedGuestId, "Extracted guestId should be null for invalid token")
    }
}
