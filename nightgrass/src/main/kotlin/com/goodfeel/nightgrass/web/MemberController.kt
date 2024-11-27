package com.goodfeel.nightgrass.web

import com.goodfeel.nightgrass.repo.ReferralRewardRepository
import com.goodfeel.nightgrass.service.IOrderService
import com.goodfeel.nightgrass.util.OrderStatus
import com.goodfeel.nightgrass.util.ReferralRewardStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.security.Principal

@Controller
@RequestMapping("/member")
class MemberController(
    private val orderService: IOrderService,
    private val rewardRepository: ReferralRewardRepository) {

    @GetMapping
    fun memberIndex(principal: Principal, model: Model): Mono<String> {
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
            .thenReturn("/member")
    }

    @GetMapping("/orders")
    fun getAllOrders(principal: Principal, model: Model): Mono<String> {
        val userId = principal.name
        return orderService.findOrderByUserId(userId).collectList()
            .doOnNext {
                model.addAttribute("orders", it)
            }.then(Mono.just("orders"))
    }

    @GetMapping("/getPendingOrders")
    fun getPendingOrders() =
        orderService.getOrderByOrderStatus(OrderStatus.PENDING)

    @GetMapping("/getProcessingOrders")
    fun getProcessingOrders() =
        orderService.getOrderByOrderStatus(OrderStatus.PROCESSING)

    @GetMapping("/getCompletedOrders")
    fun getCompletedOrders() =
        orderService.getOrderByOrderStatus(OrderStatus.COMPLETED)

}
