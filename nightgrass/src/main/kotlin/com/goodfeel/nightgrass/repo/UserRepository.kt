package com.goodfeel.nightgrass.repo

import com.goodfeel.nightgrass.data.User
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface UserRepository : ReactiveCrudRepository<User, Long> {
    fun findByOauthId(oauthId: String): Mono<User>
    fun findByGuestId(guestId: String): Mono<User>
    fun deleteByGuestId(guestId: String): Mono<Void>
}
