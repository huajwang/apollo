package com.goodfeel.nightgrass.service;

import com.goodfeel.nightgrass.data.User;
import com.goodfeel.nightgrass.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CustomReactiveOAuth2UserService extends DefaultReactiveOAuth2UserService {

    private final Logger logger = LoggerFactory.getLogger(CustomReactiveOAuth2UserService.class);

    private final UserRepository userRepository;

    public CustomReactiveOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Mono<OAuth2User> loadUser(OAuth2UserRequest userRequest) {
        return super.loadUser(userRequest)
                .flatMap(oAuth2User -> {
                    // Extract and persist user information
                    logger.debug("Exact the logged-in user's information and persist it");
                    String oauthId = oAuth2User.getAttribute("id"); // Facebook user ID
                    String name = oAuth2User.getAttribute("name");
                    String email = oAuth2User.getAttribute("email");

                    return userRepository.findByOauthId(oauthId)
                            .switchIfEmpty(Mono.defer(() -> {
                                User newUser = new User();
                                newUser.setOauthId(oauthId);
                                newUser.setNickName(name);
                                newUser.setEmail(email);
                                return Mono.just(newUser);
                            }))
                            .flatMap(user -> {
                                // Update existing user details if needed
                                user.setNickName(name);
                                user.setEmail(email);
                                return userRepository.save(user);
                            })
                            .thenReturn(oAuth2User);
                });
    }
}
