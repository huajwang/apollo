package com.goodfeel.nightgrass.web

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import reactor.core.publisher.Mono

@Controller
@RequestMapping(path = ["/home"])
class HomeController {
    @GetMapping
    fun home(model: Model?): String {
        return "home"
    }

    @GetMapping("/contact")
    fun contact(): Mono<String> {
        return Mono.just("contact")
    }
}
