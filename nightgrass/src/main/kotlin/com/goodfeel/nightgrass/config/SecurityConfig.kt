package com.goodfeel.nightgrass.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity.*
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
open class SecurityConfig {
    @Bean
    open fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http
            .authorizeExchange { exchange: AuthorizeExchangeSpec ->
                exchange
                    .pathMatchers(
                        "/", "/product/**", "/videos/**",
                        "/login**", "/error",
                        "/images/**", "/css/**", "/icons/**", "/webjars/**"
                    ).permitAll()
                    .anyExchange().authenticated()
            }
            .oauth2Login(Customizer.withDefaults<OAuth2LoginSpec>())
            .oauth2Client(Customizer.withDefaults<OAuth2ClientSpec>())
            .csrf { it.disable() }

        return http.build()
    }
}
