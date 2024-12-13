package com.goodfeel.nightgrass.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.oauth2.jwt.*
import reactor.core.publisher.Mono
import java.time.Instant

@SpringBootTest
class JwtDecodingTest(
    @Autowired private val jwtEncoder: JwtEncoder,
    @Autowired private val reactiveJwtDecoder: ReactiveJwtDecoder
) {

    @Test
    fun testJwtDecoding() {
        val issuer = "http://test-issuer.com"
        // Arrange: Create JWT claims
        val claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .subject("test-subject")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()

        // Act: Encode the JWT
        val token = jwtEncoder.encode(JwtEncoderParameters.from(claims))
        assertNotNull(token.tokenValue, "Encoded token should not be null")

        // Act: Decode the JWT
        val decodedJwtMono: Mono<Jwt> = reactiveJwtDecoder.decode(token.tokenValue)

        // Assert: Verify decoded JWT claims
        decodedJwtMono.doOnSuccess { decodedJwt ->
            assertEquals(issuer, decodedJwt.issuer.toString(), "Issuer should match")
            assertEquals("test-subject", decodedJwt.subject, "Subject should match")
            assertNotNull(decodedJwt.issuedAt, "IssuedAt should not be null")
            assertNotNull(decodedJwt.expiresAt, "ExpiresAt should not be null")
        }.block() // Blocking here for simplicity in unit tests
    }
}
