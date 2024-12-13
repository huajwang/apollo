package com.goodfeel.nightgrass.util

import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters

class CustomJwtEncoder(
    private val delegate: JwtEncoder,
    private val keyId: String,
    private val algorithm: MacAlgorithm
) : JwtEncoder {

    override fun encode(parameters: JwtEncoderParameters): Jwt {
        val customHeaders = JwsHeader.with(algorithm)
            .keyId(keyId)
            .build()

        val customParameters = JwtEncoderParameters.from(customHeaders, parameters.claims)
        return delegate.encode(customParameters)
    }
}
