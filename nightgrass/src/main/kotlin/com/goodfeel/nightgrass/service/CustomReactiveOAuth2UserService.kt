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
            .doOnSubscribe { logger.debug("Reactive pipeline subscribed!") }
            .flatMap { oAuth2User: OAuth2User ->

                val registrationId = userRequest.clientRegistration.registrationId
                logger.info("Processing OAuth2 login for provider: $registrationId")
                val oauthId = when (registrationId) {
                    "google" -> oAuth2User.getAttribute<String>("sub")
                    "facebook" -> oAuth2User.getAttribute<String>("id")
                    "wechat" -> oAuth2User.getAttribute<String>("openid")
                    else -> throw IllegalArgumentException("Unsupported OAuth2 provider: $registrationId")
                } ?: throw RuntimeException("OAuth ID is null for provider: $registrationId")

                // Extract additional attributes (if any)
                val name = when (registrationId) {
                    "google", "facebook" -> oAuth2User.getAttribute<String>("name")
                    "wechat" -> oAuth2User.getAttribute<String>("nickname")
                    else -> null
                }
                val email = oAuth2User.getAttribute<String>("email") // Email may not be available for WeChat
                userRepository.findByOauthId(oauthId)
                    .switchIfEmpty(
                        Mono.defer {
                            val newUser = User(
                                oauthId = oauthId,
                                nickName = name,
                                email = email
                            )
                            Mono.just(newUser)
                        }.doOnSuccess {
                            logger.info("Exact the logged-in user's information and persist it. oauthId: $oauthId," +
                                    " name: $name, email: $email")
                        }
                    )
                    .flatMap { user: User ->
                        // Update existing user details if needed
                        val updatedUser = user.copy(nickName = name, email = email)
                        userRepository.save(updatedUser)
                    }
                    .thenReturn(oAuth2User)
            }
    }
}
