package com.goodfeel.nightgrass.config

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder

@SpringBootTest
class JwtConfigTest {

    @InjectMocks
    private lateinit var jwtConfig: JwtConfig

    @Value("\${spring.security.oauth2.resourceserver.jwt.secret:default-secret-key}")
    private lateinit var secretKeyString: String

    @Test
    fun `should create reactiveJwtDecoder bean successfully`() {
        // Arrange
        MockitoAnnotations.openMocks(this)
        jwtConfig.secretKeyString = "test-secret-key"

        // Act
        val reactiveJwtDecoder: ReactiveJwtDecoder = jwtConfig.reactiveJwtDecoder()

        // Assert
        assertNotNull(reactiveJwtDecoder, "ReactiveJwtDecoder should not be null")
        assert(reactiveJwtDecoder is NimbusReactiveJwtDecoder)
    }
}
