package com.goodfeel.nightgrass.service

import com.goodfeel.nightgrass.data.User
import com.goodfeel.nightgrass.repo.UserRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class CustomReactiveOAuth2UserService(private val userRepository: UserRepository) : DefaultReactiveOAuth2UserService() {
    private val logger: Logger = LoggerFactory.getLogger(CustomReactiveOAuth2UserService::class.java)

    override fun loadUser(userRequest: OAuth2UserRequest): Mono<OAuth2User> {
        return super.loadUser(userRequest)
            .flatMap { oAuth2User: OAuth2User ->
                // Extract and persist user information
                logger.debug("Exact the logged-in user's information and persist it")
                val oauthId = oAuth2User.getAttribute<String>("id") // Facebook user ID
                val name = oAuth2User.getAttribute<String>("name")
                val email = oAuth2User.getAttribute<String>("email")
                userRepository.findByOauthId(oauthId)
                    .switchIfEmpty(Mono.defer {
                        val newUser = oauthId?.let {
                            User(
                                oauthId = it,
                                nickName = name,
                                email = email
                            )
                        } ?: throw RuntimeException("oauthId is null") // TODO
                        Mono.just(newUser)
                    })
                    .flatMap { user: User ->
                        // Update existing user details if needed
                        val updatedUser = user.copy(nickName = name, email = email)
                        userRepository.save(updatedUser)
                    }
                    .thenReturn(oAuth2User)
            }
    }
}
