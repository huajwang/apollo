package com.goodfeel.nightgrass.web.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import reactor.core.publisher.Mono

object Utility {
    private val logger: Logger = LoggerFactory.getLogger(Utility::class.java)

    val currentUserId: Mono<String>
        get() = ReactiveSecurityContextHolder.getContext()
            .flatMap { ctx ->
                ctx.authentication?.let { Mono.just(it.name) } ?: Mono.empty()
            }
            .defaultIfEmpty("Guest") // Guest if the user is not logged in
            .doOnNext { logger.debug("The current user is {}", it) }
}
