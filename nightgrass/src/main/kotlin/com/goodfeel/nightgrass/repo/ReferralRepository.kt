package com.goodfeel.nightgrass.repo

import com.goodfeel.nightgrass.data.ReferralLink
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface ReferralRepository : ReactiveCrudRepository<ReferralLink, Long> {
    fun findByReferralCode(referralCode: String): Mono<ReferralLink>

    fun findBySharerId(sharerId: Long): Mono<ReferralLink>
}
