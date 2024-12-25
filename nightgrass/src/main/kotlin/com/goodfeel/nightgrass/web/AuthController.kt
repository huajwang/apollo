package com.goodfeel.nightgrass.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import reactor.core.publisher.Mono

@Controller
class AuthController {

    @GetMapping("/login")
    fun endUserLoginPage(): Mono<String> {
        return Mono.just("login")
    }

    @GetMapping("/admin/login")
    fun adminLoginPage(): String {
        return "admin/admin-login" // Refers to the admin-login.html template
    }
}
