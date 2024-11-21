package com.goodfeel.nightgrass.repo

import com.goodfeel.nightgrass.data.ReferralReward
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux

interface ReferralRewardRepository : ReactiveCrudRepository<ReferralReward, Long> {
    fun findBySharerId(sharerId: String): Flux<ReferralReward>
}
