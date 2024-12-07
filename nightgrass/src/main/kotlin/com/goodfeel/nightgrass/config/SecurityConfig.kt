package com.goodfeel.nightgrass.config

import com.goodfeel.nightgrass.serviceImpl.CartService
import com.goodfeel.nightgrass.web.CartMergeAuthenticationSuccessHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity.*
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
open class SecurityConfig(
    private val cartService: CartService
) {
    @Bean
    open fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http
            .authorizeExchange { exchange: AuthorizeExchangeSpec ->
                exchange
                    .pathMatchers(
                        "/", "/product/**", "/videos/**",
                        "/login**", "/error", "/cart/**",
                        "/images/**", "/css/**", "/icons/**", "/js/**", "/webjars/**",
                    ).permitAll()
                    .anyExchange().authenticated()
            }
            .oauth2Login{
                // Use custom handler
                it.authenticationSuccessHandler(CartMergeAuthenticationSuccessHandler(cartService))
            }
            .oauth2Client(Customizer.withDefaults<OAuth2ClientSpec>())
            .csrf { it.disable() }

        return http.build()
    }
}
