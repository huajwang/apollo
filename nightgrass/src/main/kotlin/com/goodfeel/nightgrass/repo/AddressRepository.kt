package com.goodfeel.nightgrass.repo

import com.goodfeel.nightgrass.data.Address
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface AddressRepository : ReactiveCrudRepository<Address, Long> {
    fun findByUserId(userId: Long): Flux<Address>
    fun findByUserIdAndIsDefaultTrue(userId: Long): Mono<Address>
}
