package com.goodfeel.nightgrass.serviceImpl

import com.goodfeel.nightgrass.data.Referral
import com.goodfeel.nightgrass.data.ReferralReward
import com.goodfeel.nightgrass.repo.ReferralRepository
import com.goodfeel.nightgrass.repo.ReferralRewardRepository
import com.goodfeel.nightgrass.util.Constant
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(ReferralService::class.java)

    fun getReferralLinkForUser(userId: String): Mono<String> {
        return referralRepository.findBySharerId(userId)
            .switchIfEmpty(
                referralRepository.save(
                    Referral(
                        sharerId = userId,
                        referralCode = UUID.randomUUID().toString()
                    )
                )
            ).doOnNext { logger.debug("Saved referral from user: ${it.sharerId}") }
            .map { "${Constant.baseUrl}/referral/${it.referralCode}" }
    }

    fun addReward(sharerId: String, orderId: Long, orderTotal: BigDecimal): Mono<ReferralReward> {
        val rewardAmount = orderTotal.multiply(BigDecimal.valueOf(0.1))
        return referralRewardRepository.save(
            ReferralReward(
                sharerId = sharerId,
                orderId = orderId,
                rewardAmount = rewardAmount
            )
        )
    }

    fun getRewardsForSharer(sharerId: String): Flux<ReferralReward> {
        return referralRewardRepository.findBySharerId(sharerId)
    }
}
