package com.goodfeel.nightgrass.config

import com.goodfeel.nightgrass.service.OrderService
import com.goodfeel.nightgrass.service.UserService
import com.goodfeel.nightgrass.serviceImpl.CartService
import com.goodfeel.nightgrass.serviceImpl.GuestService
import com.goodfeel.nightgrass.web.MergeAuthenticationSuccessHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity.*
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import java.net.URI

@Configuration
@EnableWebFluxSecurity
open class SecurityConfig(
    private val cartService: CartService,
    private val guestService: GuestService,
    private val orderService: OrderService,
    private val userService: UserService,
    private val reactiveJwtDecoder: ReactiveJwtDecoder,
    private val adminAuthenticationManager: AdminAuthenticationManager
) {

    @Bean
    open fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http
            .authorizeExchange { exchange: AuthorizeExchangeSpec ->
                exchange
                    .pathMatchers(
                        "/", "/product/**", "/videos/**", "/blog/**", "/buynow", "/pay/**", "/home/**",
                        "/login", "/admin/login", "/error", "/cart/**", "/checkout", "/update-user-info",
                        "/images/**", "/css/**", "/icons/**", "/js/**", "/webjars/**",
                    ).permitAll()
                    // Admin paths, restricted to ROLE_ADMIN
                    .pathMatchers("/admin/**").hasRole("ADMIN")
                    // All other paths require authentication
                    .anyExchange().authenticated()
            }
            .oauth2Login{
                // Use custom handler
                it.authenticationSuccessHandler(
                    MergeAuthenticationSuccessHandler(
                        cartService, guestService, orderService, userService))
            }
            .oauth2Client(Customizer.withDefaults<OAuth2ClientSpec>())

            .formLogin {
                it.loginPage("/admin/login")
                    .authenticationManager(adminAuthenticationManager)
                    .authenticationSuccessHandler(adminSuccessHandler()) // Custom success handler
                    .authenticationFailureHandler(adminFailureHandler()) // Custom failure handler
            }

            // Enable JWT validation for incoming requests
            .oauth2ResourceServer { resourceServer ->
                resourceServer.jwt { jwtConfigurer ->
                    jwtConfigurer.jwtDecoder(reactiveJwtDecoder) // Explicitly specify the JwtDecoder bean
                }
            }
            .csrf { it.disable() }

        return http.build()
    }

    @Bean
    open fun adminSuccessHandler(): ServerAuthenticationSuccessHandler {
        return ServerAuthenticationSuccessHandler { webFilterExchange, _ ->
            val response = webFilterExchange.exchange.response
            response.statusCode = HttpStatus.FOUND
            response.headers.location = URI.create("/admin/products") // Redirect to admin dashboard
            response.setComplete()
        }
    }

    @Bean
    open fun adminFailureHandler(): ServerAuthenticationFailureHandler {
        return ServerAuthenticationFailureHandler { webFilterExchange, _ ->
            val response = webFilterExchange.exchange.response
            response.statusCode = HttpStatus.FOUND
            response.headers.location = URI.create("/admin/login?error=true")
            response.setComplete()
        }
    }

}
