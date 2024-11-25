package com.goodfeel.nightgrass.web

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping(path = ["/"])
class HomeController {
    @GetMapping
    fun home(model: Model?): String {
        return "home"
    }
}
