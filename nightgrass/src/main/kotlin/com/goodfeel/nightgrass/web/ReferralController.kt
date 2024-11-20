package com.goodfeel.nightgrass.web

import com.goodfeel.nightgrass.serviceImpl.ReferralService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import reactor.core.publisher.Mono
import java.security.Principal

@Controller
class ReferralController(private val referralService: ReferralService) {

    @GetMapping("/referral/link")
    fun getReferralLink(principal: Principal, model: Model): Mono<String> {
        val userId = principal.name.toLong()  // Convert username to user ID
        val referralLink = referralService.getReferralLinkForUser(userId)

        model.addAttribute("referralLink", referralLink)
        return Mono.just("referral")
    }
}
