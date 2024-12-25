package com.goodfeel.nightgrass.repo.admin

import com.goodfeel.nightgrass.data.admin.Admin
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface AdminRepository : ReactiveCrudRepository<Admin, Long> {
    fun findByUsername(username: String): Mono<Admin>
}
