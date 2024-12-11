package com.goodfeel.nightgrass.config

import com.goodfeel.nightgrass.service.OrderService
import com.goodfeel.nightgrass.service.UserService
import com.goodfeel.nightgrass.serviceImpl.CartService
import com.goodfeel.nightgrass.serviceImpl.GuestService
import com.goodfeel.nightgrass.web.MergeAuthenticationSuccessHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity.*
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
open class SecurityConfig(
    private val cartService: CartService,
    private val guestService: GuestService,
    private val orderService: OrderService,
    private val userService: UserService,
    private val reactiveJwtDecoder: ReactiveJwtDecoder
) {

    @Bean
    open fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http
            .authorizeExchange { exchange: AuthorizeExchangeSpec ->
                exchange
                    .pathMatchers(
                        "/", "/product/**", "/videos/**",
                        "/login**", "/error", "/cart/**", "/checkout", "/update-user-info",
                        "/images/**", "/css/**", "/icons/**", "/js/**", "/webjars/**",
                    ).permitAll()
                    .anyExchange().authenticated()
            }
            .oauth2Login{
                // Use custom handler
                it.authenticationSuccessHandler(
                    MergeAuthenticationSuccessHandler(
                        cartService, guestService, orderService, userService))
            }
            .oauth2Client(Customizer.withDefaults<OAuth2ClientSpec>())
            // Enable JWT validation for incoming requests
            .oauth2ResourceServer { resourceServer ->
                resourceServer.jwt { jwtConfigurer ->
                    jwtConfigurer.jwtDecoder(reactiveJwtDecoder) // Explicitly specify the JwtDecoder bean
                }
            }
            .csrf { it.disable() }

        return http.build()
    }

}
