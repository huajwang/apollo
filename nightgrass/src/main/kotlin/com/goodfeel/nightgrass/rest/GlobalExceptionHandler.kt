package com.goodfeel.nightgrass.rest

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ServerWebInputException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ServerWebInputException::class)
    fun handleServerWebInputException(ex: ServerWebInputException): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.badRequest().body(
            mapOf("error" to "Invalid input", "details" to (ex.message ?: "Unknown error"))
        )
    }
}
