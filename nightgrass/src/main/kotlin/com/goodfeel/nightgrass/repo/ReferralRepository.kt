package com.goodfeel.nightgrass.repo

import com.goodfeel.nightgrass.data.Referral
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface ReferralRepository : ReactiveCrudRepository<Referral, Long> {
    fun findByReferralCode(referralCode: String): Mono<Referral>

    fun findBySharerId(sharerId: Long): Mono<Referral>
}
