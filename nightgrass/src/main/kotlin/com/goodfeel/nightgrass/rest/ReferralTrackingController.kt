package com.goodfeel.nightgrass.rest

import com.goodfeel.nightgrass.serviceImpl.ReferralTrackingService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.net.URI
import org.springframework.http.HttpStatus

@RestController
class ReferralTrackingController(private val referralTrackingService: ReferralTrackingService) {

    @GetMapping("/referral/{referralCode}")
    fun trackReferral(
        @PathVariable referralCode: String,
        @RequestParam(required = false) target: String?,
        exchange: ServerWebExchange
    ): Mono<Void> {
        return referralTrackingService.trackReferral(referralCode, exchange)
            .then(Mono.defer {
                exchange.response.statusCode = HttpStatus.FOUND // HTTP 302
                val redirectLocation = target ?: "/"
                exchange.response.headers.location = URI.create(redirectLocation)
                Mono.empty()
            })
    }
}
