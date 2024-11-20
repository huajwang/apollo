package com.goodfeel.nightgrass.serviceImpl

import com.goodfeel.nightgrass.data.ReferralLink
import com.goodfeel.nightgrass.data.ReferralReward
import com.goodfeel.nightgrass.repo.ReferralRepository
import com.goodfeel.nightgrass.repo.ReferralRewardRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.publisher.Flux
import java.math.BigDecimal
import java.util.UUID

@Service
class ReferralService(
    private val referralRepository: ReferralRepository,
    private val referralRewardRepository: ReferralRewardRepository
) {
    fun getReferralLinkForUser(userId: Long): Mono<String> {
        return referralRepository.findBySharerId(userId)
            .switchIfEmpty(
                referralRepository.save(
                    ReferralLink(
                        sharerId = userId,
                        referralCode = UUID.randomUUID().toString()
                    )
                )
            )
            .map { "https://localhost:8443/referral/${it.referralCode}" }
    }

    fun addReward(sharerId: Long, orderId: Long, orderTotal: BigDecimal): Mono<ReferralReward> {
        val rewardAmount = orderTotal.multiply(BigDecimal.valueOf(0.1))
        return referralRewardRepository.save(
            ReferralReward(
                sharerId = sharerId,
                orderId = orderId,
                rewardAmount = rewardAmount
            )
        )
    }

    fun getRewardsForSharer(sharerId: Long): Flux<ReferralReward> {
        return referralRewardRepository.findBySharerId(sharerId)
    }
}
