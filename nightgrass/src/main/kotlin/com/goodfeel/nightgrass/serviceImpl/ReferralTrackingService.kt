package com.goodfeel.nightgrass.serviceImpl

import com.goodfeel.nightgrass.data.ReferralReward
import com.goodfeel.nightgrass.repo.ReferralRepository
import com.goodfeel.nightgrass.repo.ReferralRewardRepository
import com.goodfeel.nightgrass.util.OrderStatus
import com.goodfeel.nightgrass.util.ReferralRewardStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.math.BigDecimal



@Service
class ReferralTrackingService(
    private val referralRepository: ReferralRepository,
    private val referralRewardRepository: ReferralRewardRepository) {

    private val logger = LoggerFactory.getLogger(ReferralTrackingService::class.java)

    fun trackReferral(referralCode: String, exchange: ServerWebExchange): Mono<Void> {
        return referralRepository.findByReferralCode(referralCode)
            .flatMap { referral ->
                exchange.session.flatMap { session ->
                    logger.debug("Put sharedId: ${referral.sharerId} in session")
                    session.attributes["sharerId"] = referral.sharerId
                    Mono.empty()
                }
            }
    }

    fun rewardSharer(
        sharerId: String, reward: BigDecimal, orderId: Long, referralRewardStatus: ReferralRewardStatus): Mono<Void> {
        logger.debug("Reward sharerId $sharerId on order $orderId is $reward")
        val referralReward = ReferralReward(
            null, sharerId, orderId, rewardAmount = reward, referralRewardStatus = referralRewardStatus)
        return referralRewardRepository.save(referralReward).then()
    }

}
