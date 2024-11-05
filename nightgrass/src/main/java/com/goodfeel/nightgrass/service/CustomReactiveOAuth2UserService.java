package com.goodfeel.nightgrass.service;

import com.goodfeel.nightgrass.data.User;
import com.goodfeel.nightgrass.repo.UserRepo;
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CustomReactiveOAuth2UserService extends DefaultReactiveOAuth2UserService {

    private final UserRepo userRepository;

    public CustomReactiveOAuth2UserService(UserRepo userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Mono<OAuth2User> loadUser(OAuth2UserRequest userRequest) {
        return super.loadUser(userRequest)
                .flatMap(oAuth2User -> {
                    // Extract and persist user information
                    String oauthId = oAuth2User.getAttribute("id"); // Facebook user ID
                    String name = oAuth2User.getAttribute("name");
                    String email = oAuth2User.getAttribute("email");

                    return userRepository.findByOauthId(oauthId)
                            .switchIfEmpty(Mono.defer(() -> {
                                User newUser = new User();
                                newUser.setOauthId(oauthId);
                                newUser.setName(name);
                                newUser.setEmail(email);
                                return userRepository.save(newUser);
                            }))
                            .flatMap(existingUser -> {
                                // Update existing user details if needed
                                existingUser.setName(name);
                                existingUser.setEmail(email);
                                return userRepository.save(existingUser);
                            })
                            .thenReturn(oAuth2User);
                });
    }
}
