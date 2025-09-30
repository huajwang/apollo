package com.goodfeel.nightgrass.web.legal

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping


@Controller
@RequestMapping("/legal")
class LegalController {

    @GetMapping("/terms-of-service")
    fun termsOfService(): String {
        return "legal/terms-of-service"
    }

    @GetMapping("/privacy-policy")
    fun privacyPolicy(): String {
        return "legal/privacy-policy"
    }
}
