package com.goodfeel.nightgrass.serviceImpl

import com.goodfeel.nightgrass.repo.ReferralRepository
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Service
class ReferralTrackingService(private val referralRepository: ReferralRepository) {

    fun trackReferral(referralCode: String, exchange: ServerWebExchange): Mono<Void> {
        return referralRepository.findByReferralCode(referralCode)
            .flatMap { referral ->
                exchange.session.flatMap { session ->
                    session.attributes["sharerId"] = referral.sharerId
                    Mono.empty()
                }
            }
    }
}
