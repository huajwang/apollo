package com.goodfeel.nightgrass.web.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

public class Utility {

    private static final Logger logger = LoggerFactory.getLogger(Utility.class);

    public static Mono<String> getCurrentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .defaultIfEmpty("Guest") // Guest if the user is not logged in
                .doOnSuccess(it -> logger.debug("The current user is {}", it));
    }
}
