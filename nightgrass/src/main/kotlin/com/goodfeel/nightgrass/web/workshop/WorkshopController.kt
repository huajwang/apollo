package com.goodfeel.nightgrass.web.workshop

import com.goodfeel.nightgrass.service.workshop.WorkshopService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import reactor.core.publisher.Mono

@Controller
@RequestMapping("/workshop")
class WorkshopController(
    private val workshopService: WorkshopService
) {


    @GetMapping("/incoming-workshop")
    fun showEvent(model: Model): Mono<String> {
        return workshopService.getHomePageWorkshop().map {
            model.addAttribute("workshop", it)
            "workshop/incoming"
        }
    }
}
