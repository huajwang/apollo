package com.goodfeel.nightgrass.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import java.time.Instant


@SpringBootTest
class JwtEncodingTest(@Autowired private val jwtEncoder: JwtEncoder) {

    @Test
    fun testJwtEncoding() {
        val claims = JwtClaimsSet.builder()
            .issuer("test-issuer")
            .subject("test-subject")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()

        try {
            val token = jwtEncoder.encode(JwtEncoderParameters.from(claims))
            println("Encoded JWT: ${token.tokenValue}")
            assertNotNull(token.tokenValue)
        } catch (e: Exception) {
            fail("JWT encoding failed: ${e.message}")
        }
    }
}

