package com.goodfeel.nightgrass.config

import com.goodfeel.nightgrass.service.AdminDetailsService
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class AdminAuthenticationManager(
    private val adminDetailsService: AdminDetailsService,
    private val passwordEncoder: PasswordEncoder
) : ReactiveAuthenticationManager {

    override fun authenticate(authentication: Authentication): Mono<Authentication> {
        val username = authentication.name
        val rawPassword = authentication.credentials.toString()

        return adminDetailsService.findByUsername(username)
            .flatMap { userDetails ->
                if (passwordEncoder.matches(rawPassword, userDetails.password)) {
                    Mono.just(
                        UsernamePasswordAuthenticationToken(
                            userDetails.username,
                            userDetails.password,
                            userDetails.authorities
                        ) as Authentication
                    )
                } else {
                    Mono.error(BadCredentialsException("Invalid credentials"))
                }
            }
            .switchIfEmpty(Mono.error(BadCredentialsException("Admin not found")))
    }
}
