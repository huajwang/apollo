package com.goodfeel.nightgrass.web.util

import com.goodfeel.nightgrass.util.Constant
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ThreadLocalRandom

object Utility {
    private val logger: Logger = LoggerFactory.getLogger(Utility::class.java)

    val currentUserId: Mono<String>
        get() = ReactiveSecurityContextHolder.getContext()
            .flatMap { ctx ->
                ctx.authentication?.let { Mono.just(it.name) } ?: Mono.empty()
            }
            .defaultIfEmpty(Constant.GUEST) // Guest if the user is not logged in
            .doOnNext { logger.debug("The current user is {}", it) }

    const val HST = 0.13

    // Helper method to generate a human-readable order ID with date/time and a unique suffix
    fun generateOrderNo(): String {
        // Format current date-time to a string
        val dateTimePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
        // Generate a random 4-digit number as a suffix to ensure uniqueness
        val randomSuffix = ThreadLocalRandom.current().nextInt(1000, 9999)
        return dateTimePart + randomSuffix
    }

}
