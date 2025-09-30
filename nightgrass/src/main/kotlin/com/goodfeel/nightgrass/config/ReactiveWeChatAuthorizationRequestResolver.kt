package com.goodfeel.nightgrass.config

import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import org.springframework.web.util.UriComponentsBuilder

class ReactiveWeChatAuthorizationRequestResolver(
    clientRegistrationRepository: ReactiveClientRegistrationRepository
) : ServerOAuth2AuthorizationRequestResolver {



    // Initialize the default resolver with the standard path matcher
    private val defaultResolver = DefaultServerOAuth2AuthorizationRequestResolver(clientRegistrationRepository)

    override fun resolve(exchange: ServerWebExchange): Mono<OAuth2AuthorizationRequest> {
        // Use the default resolver and customize for WeChat
        return defaultResolver.resolve(exchange).flatMap { originalRequest ->
            customizeAuthorizationRequestIfWeChat(originalRequest)
        }
    }

    override fun resolve(exchange: ServerWebExchange, clientRegistrationId: String): Mono<OAuth2AuthorizationRequest> {
        // Use the default resolver and customize for WeChat
        return defaultResolver.resolve(exchange, clientRegistrationId).flatMap { originalRequest ->
            customizeAuthorizationRequestIfWeChat(originalRequest)
        }
    }

    private fun customizeAuthorizationRequestIfWeChat(request: OAuth2AuthorizationRequest): Mono<OAuth2AuthorizationRequest> {
        val registrationId = request.attributes["registration_id"] as? String
        if (registrationId == "wechat") {
            // Modify the authorization URI for WeChat
            val modifiedUri = UriComponentsBuilder.fromUriString(request.authorizationRequestUri)
                .replaceQueryParam("client_id", null) // Remove `client_id`
                .queryParam("appid", request.clientId) // Add `appid`
                .build()
                .toUriString()

            // Build and return the customized authorization request
            val modifiedRequest = OAuth2AuthorizationRequest.from(request)
                .authorizationRequestUri(modifiedUri)
                .build()

            return Mono.just(modifiedRequest)
        }

        // Return the original request for non-WeChat providers
        return Mono.just(request)
    }
}
