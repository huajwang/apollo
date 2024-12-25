package com.goodfeel.nightgrass.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class AuthController {

    @GetMapping("/admin/login")
    fun adminLoginPage(): String {
        return "admin/admin-login" // Refers to the admin-login.html template
    }
}
