package com.goodfeel.nightgrass.scratch;

import reactor.core.publisher.Mono;

class Scratch {
    public static void main(String[] args) {
        // Create a Mono that immediately evaluates the current time
        Mono<Long> immediateMono = Mono.just(System.currentTimeMillis());

        // Create a Mono that defers the evaluation of the current time until subscription
        Mono<Long> deferredMono = Mono.defer(() -> Mono.just(System.currentTimeMillis()));

        // Subscribe to both Monos and print their values
        immediateMono.subscribe(time -> System.out.println("Immediate Mono: " + time));
        deferredMono.subscribe(time -> System.out.println("Deferred Mono: " + time));

        // Adding a small delay before re-subscribing to demonstrate defer behavior
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Re-subscribe to both Monos and print their values
        immediateMono.subscribe(time -> System.out.println("Immediate Mono (again): " + time));
        deferredMono.subscribe(time -> System.out.println("Deferred Mono (again): " + time));
    }
}