package com.goodfeel.nightgrass.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping(path = "/checkout")
public class CheckoutController {

    @GetMapping
    public Mono<String> checkout(Model model) {
        model.addAttribute("STRIPE_PUBLIC_KEY",
                "");
        return Mono.just("checkout");
    }
}
