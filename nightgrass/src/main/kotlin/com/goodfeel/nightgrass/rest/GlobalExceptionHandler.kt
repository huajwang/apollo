package com.goodfeel.nightgrass.rest

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ServerWebInputException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ServerWebInputException::class)
    fun handleServerWebInputException(ex: ServerWebInputException): ResponseEntity<Map<String, Any>> {
        if (ex.cause != null) {
            logger.error("Root cause: {}", ex.cause?.message, ex.cause)
        }
        return ResponseEntity.badRequest().body(
            mapOf(
                "error" to "Invalid input",
                "details" to (ex.cause?.message ?: ex.message ?: "Unknown error")
            )
        )
    }
}

