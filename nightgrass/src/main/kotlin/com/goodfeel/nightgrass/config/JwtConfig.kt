package com.goodfeel.nightgrass.config

import com.goodfeel.nightgrass.util.CustomJwtEncoder
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.*
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.*

@Configuration
open class JwtConfig {

    @Value("\${spring.security.oauth2.resourceserver.jwt.secret}")
    internal lateinit var secretKeyString: String

    @Bean
    open fun jwtEncoder(): JwtEncoder {
        val jwk = OctetSequenceKey.Builder(secretKeyString.toByteArray())
            .algorithm(JWSAlgorithm.HS256)         // Algorithm matches MacAlgorithm.HS256
            .keyUse(KeyUse.SIGNATURE)              // Key use is SIGNATURE
            .keyID("shared-secret-key-id")         // Key ID must match JwsHeader
            .build()

        val jwkSet = JWKSet(listOf(jwk))
        val jwkSource: JWKSource<SecurityContext> = ImmutableJWKSet(jwkSet)

        // Create the wrapped encoder with enforced keyID and algorithm
        val delegate = NimbusJwtEncoder(jwkSource)
        return CustomJwtEncoder(delegate, "shared-secret-key-id", MacAlgorithm.HS256)
    }


    @Bean
    open fun reactiveJwtDecoder(): ReactiveJwtDecoder {
        val secretKey: SecretKey = SecretKeySpec(secretKeyString.toByteArray(), "HmacSHA256")
        return NimbusReactiveJwtDecoder.withSecretKey(secretKey).build()
    }

}
