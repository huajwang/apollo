package com.goodfeel.nightgrass.web

import com.goodfeel.nightgrass.repo.ReferralRewardRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import reactor.core.publisher.Mono
import java.security.Principal

@Controller
class RewardDashboardController(private val rewardRepository: ReferralRewardRepository) {

    @GetMapping("/member")
    fun getRewards(principal: Principal, model: Model): Mono<String> {
        val userId = principal.name
        val rewards = rewardRepository.findBySharerId(userId)
        model.addAttribute("rewards", rewards)
        return Mono.just("/member")
    }
}
