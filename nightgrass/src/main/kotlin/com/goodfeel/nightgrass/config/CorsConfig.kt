package com.goodfeel.nightgrass.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
open class CorsConfig {

    @Bean
    open fun corsConfigurationSource(): CorsConfigurationSource {
        val cors = CorsConfiguration()
        cors.allowedOrigins = listOf("https://*.yaojiabuy.com", "http://localhost:4200")
        cors.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        cors.allowedHeaders = listOf("*")
        cors.allowCredentials = true
        cors.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", cors)
        return source
    }
}

