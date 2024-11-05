package com.goodfeel.nightgrass.rest;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class UserController {

    @GetMapping("/user")
    public Mono<Map<String, Object>> user(@AuthenticationPrincipal OAuth2User oauth2User) {
        return Mono.just(oauth2User.getAttributes());
    }
}
