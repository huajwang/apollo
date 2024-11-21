package com.goodfeel.nightgrass.web

import com.goodfeel.nightgrass.repo.ReferralRewardRepository
import com.goodfeel.nightgrass.util.ReferralRewardStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.security.Principal

@Controller
class MemberController(private val rewardRepository: ReferralRewardRepository) {

    @GetMapping("/member")
    fun getRewards(principal: Principal, model: Model): Mono<String> {
        val userId = principal.name

        val rewardsFlux = rewardRepository.findBySharerId(userId)

        return rewardsFlux
            .collectList() // Collect rewards into a list for the model
            .doOnNext { rewards ->
                model.addAttribute("rewards", rewards)
            }
            .then(
                rewardsFlux
                    .filter {
                        it.referralRewardStatus in listOf(ReferralRewardStatus.APPROVED, ReferralRewardStatus.PENDING)
                    }
                    .map { it.rewardAmount }
                    .reduce(BigDecimal.ZERO) { total, amount -> total.add(amount) } // Calculate total reactively
                    .doOnNext { rewardsTotal ->
                        model.addAttribute("rewardsTotal", rewardsTotal)
                    }
            )
            .thenReturn("/member") // Return the view name
    }
}
