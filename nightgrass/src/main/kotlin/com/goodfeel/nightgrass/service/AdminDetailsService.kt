package com.goodfeel.nightgrass.service

import com.goodfeel.nightgrass.repo.admin.AdminRepository
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class AdminDetailsService(
    private val adminRepository: AdminRepository
) : ReactiveUserDetailsService {
    override fun findByUsername(username: String): Mono<UserDetails> {
        return adminRepository.findByUsername(username)
            .map { admin ->
                User.builder()
                    .username(admin.username)
                    .password(admin.password) // Hashed password
                    .roles(admin.role) // Ensure this matches ROLE_ADMIN
                    .build()
            }
    }
}
