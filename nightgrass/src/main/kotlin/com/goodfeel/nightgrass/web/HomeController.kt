package com.goodfeel.nightgrass.web

import com.goodfeel.nightgrass.serviceImpl.ProductService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import reactor.core.publisher.Mono

@Controller
@RequestMapping(path = ["/home"])
class HomeController(
    private val productService: ProductService
) {
    @GetMapping
    fun home(model: Model): Mono<String> {
        val bigHitsMono = productService.getTop3BigHits().collectList()
        val popularsMono = productService.getTop8PopularOrNewProducts().collectList()
        return bigHitsMono.zipWith(popularsMono) { bigHits, populars ->
            model.addAttribute("bigHits", bigHits)
            model.addAttribute("populars", populars)
            "home"
        }
    }

    @GetMapping("/contact")
    fun contact(): Mono<String> {
        return Mono.just("contact")
    }
}
