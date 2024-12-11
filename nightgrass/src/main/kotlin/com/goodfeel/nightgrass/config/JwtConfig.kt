package com.goodfeel.nightgrass.config

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.OctetSequenceKey
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Configuration
open class JwtConfig {

    @Value("\${spring.security.oauth2.resourceserver.jwt.secret}")
    private lateinit var secretKeyString: String

    @Bean
    open fun jwtEncoder(): JwtEncoder {
        val jwk = OctetSequenceKey.Builder(secretKeyString.toByteArray())
            .algorithm(com.nimbusds.jose.JWSAlgorithm.HS256)
            .build()
        val jwkSet = JWKSet(jwk)
        val jwkSource: com.nimbusds.jose.jwk.source.JWKSource<com.nimbusds.jose.proc.SecurityContext> =
            com.nimbusds.jose.jwk.source.ImmutableJWKSet(jwkSet)

        return NimbusJwtEncoder(jwkSource)
    }

    @Bean
    open fun reactiveJwtDecoder(): ReactiveJwtDecoder {
        val secretKey: SecretKey = SecretKeySpec(secretKeyString.toByteArray(), "HmacSHA256")
        return NimbusReactiveJwtDecoder.withSecretKey(secretKey).build()
    }
}
