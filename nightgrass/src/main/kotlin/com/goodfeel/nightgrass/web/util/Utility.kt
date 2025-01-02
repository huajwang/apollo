package com.goodfeel.nightgrass.web.util

import com.goodfeel.nightgrass.util.Constant
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ThreadLocalRandom

@Component
object Utility : ApplicationContextAware {
    private val logger: Logger = LoggerFactory.getLogger(Utility::class.java)

    private lateinit var applicationContext: ApplicationContext

    override fun setApplicationContext(context: ApplicationContext) {
        applicationContext = context
    }

    val OSS_BUCKET_NAME: String
        get() = applicationContext.environment.getProperty("oss.bucket-name")
            ?: throw IllegalStateException("oss.bucket-name property is not set")

    val currentUserId: Mono<String>
        get() = ReactiveSecurityContextHolder.getContext()
            .flatMap { ctx ->
                ctx.authentication?.let { Mono.just(it.name) } ?: Mono.empty()
            }
            .defaultIfEmpty(Constant.GUEST) // Guest if the user is not logged in
            .doOnNext { logger.debug("The current user is {}", it) }

    const val HST = 0.13
    const val REFERRAL_REWARD_RATE = 0.1
    const val APPLICATION_NAME = "yaojiabuy"
    const val OSS_ENDPOINT = "oss-cn-shenzhen.aliyuncs.com"
    // const val OSS_BUCKET_NAME = "yaojiabuy"


    // Helper method to generate a human-readable order ID with date/time and a unique suffix
    fun generateOrderNo(): String {
        // Format current date-time to a string
        val dateTimePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
        // Generate a random 4-digit number as a suffix to ensure uniqueness
        val randomSuffix = ThreadLocalRandom.current().nextInt(1000, 9999)
        return dateTimePart + randomSuffix
    }

    fun generateMediaUrl(objectKey: String): String {
        val bucketUrl = "https://${OSS_BUCKET_NAME}.${OSS_ENDPOINT}"
        return "$bucketUrl/$objectKey"
    }

    fun removeOssPrefix(objectNames: List<String>): List<String> {
        val prefix = "https://${OSS_BUCKET_NAME}.${OSS_ENDPOINT}/"
        return objectNames.map { objectName ->
            if (objectName.startsWith(prefix)) {
                objectName.removePrefix(prefix)
            } else {
                objectName
            }
        }
    }

}
