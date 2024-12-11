package com.goodfeel.nightgrass.web.util

import com.goodfeel.nightgrass.util.Constant
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import reactor.core.publisher.Mono

object Utility {
    private val logger: Logger = LoggerFactory.getLogger(Utility::class.java)

    private val secretKey = "your-secret-key"

    val currentUserId: Mono<String>
        get() = ReactiveSecurityContextHolder.getContext()
            .flatMap { ctx ->
                ctx.authentication?.let { Mono.just(it.name) } ?: Mono.empty()
            }
            .defaultIfEmpty(Constant.GUEST) // Guest if the user is not logged in
            .doOnNext { logger.debug("The current user is {}", it) }

}
