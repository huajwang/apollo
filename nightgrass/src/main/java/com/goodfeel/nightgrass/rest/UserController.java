package com.goodfeel.nightgrass.rest;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class UserController {

    @GetMapping("/user")
    public Mono<String> user(@AuthenticationPrincipal OAuth2User oauth2User) {
        return Mono.just("User info: " + oauth2User.getAttributes().toString());
    }
}
