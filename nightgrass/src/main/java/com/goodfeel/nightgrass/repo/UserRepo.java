package com.goodfeel.nightgrass.repo;

import com.goodfeel.nightgrass.data.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserRepo extends ReactiveCrudRepository<User, Long> {

    Mono<User> findByOauthId(String oauthId);
}
