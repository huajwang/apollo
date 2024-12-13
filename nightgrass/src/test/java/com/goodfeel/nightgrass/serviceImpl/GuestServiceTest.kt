package com.goodfeel.nightgrass.serviceImpl

import com.goodfeel.nightgrass.data.User
import com.goodfeel.nightgrass.repo.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*
import org.springframework.http.HttpCookie
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.util.LinkedMultiValueMap
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.security.Principal

class GuestServiceTest {

    private lateinit var guestService: GuestService
    private lateinit var jwtService: JwtService
    private lateinit var userRepository: UserRepository
    private lateinit var request: ServerHttpRequest
    private lateinit var response: ServerHttpResponse

    @BeforeEach
    fun setUp() {
        jwtService = mock(JwtService::class.java)
        userRepository = mock(UserRepository::class.java)
        request = mock(ServerHttpRequest::class.java)
        response = mock(ServerHttpResponse::class.java)
        guestService = GuestService(jwtService, userRepository)
    }

    @Test
    fun `retrieveUserGuestOrCreate should return logged-in user`() {
        val principal = mock(Principal::class.java)
        `when`(principal.name).thenReturn("user-id")
        `when`(userRepository.findByOauthId("user-id")).thenReturn(Mono.just(User(oauthId = "user-id")))

        StepVerifier.create(guestService.retrieveUserGuestOrCreate(principal, request, response))
            .expectNextMatches { user -> user.oauthId == "user-id" }
            .verifyComplete()

        verify(userRepository).findByOauthId("user-id")
        verifyNoInteractions(request, response)
    }

    @Test
    fun `retrieveUserGuestOrCreate should create and return guest user`() {
        val cookies = LinkedMultiValueMap<String, HttpCookie>()
        `when`(request.cookies).thenReturn(cookies)
        `when`(request.headers).thenReturn(HttpHeaders())
        `when`(userRepository.findByGuestId(anyString())).thenReturn(Mono.empty())
        `when`(userRepository.save(any(User::class.java)))
            .thenAnswer { Mono.just(it.getArgument<User>(0)) }
        `when`(response.headers).thenReturn(HttpHeaders())

        StepVerifier.create(guestService.retrieveUserGuestOrCreate(null, request, response))
            .expectNextMatches { user -> user.guestId != null }
            .verifyComplete()

        verify(userRepository).save(any(User::class.java))
        verify(response).addCookie(any(ResponseCookie::class.java))
        verify(response).headers
    }

    @Test
    fun `getGuestId should return guestId from cookie if guestId only presented in cookie`() {
        val cookies = LinkedMultiValueMap<String, HttpCookie>()
        cookies.add("guestId", HttpCookie("guestId", "guest-id"))
        `when`(request.cookies).thenReturn(cookies)
        `when`(request.headers).thenReturn(HttpHeaders())

        StepVerifier.create(guestService.getGuestId(request))
            .expectNext("guest-id")
            .verifyComplete()
        verify(request, times(0)).headers
        verify(jwtService, times(0)).validateAndExtractGuestId("token")
    }

    @Test
    fun `getGuestId should return guestId from cookie if guestId presented in both cookie and header`() {
        val cookie = mock(ResponseCookie::class.java)
        val cookies = LinkedMultiValueMap<String, HttpCookie>()
        cookies.add("guestId", HttpCookie("guestId", "mocked-guest-id"))
        `when`(cookie.value).thenReturn("mocked-guest-id")
        `when`(request.cookies).thenReturn(cookies)
        `when`(request.headers).thenReturn(HttpHeaders().apply {
            add(HttpHeaders.AUTHORIZATION, "Bearer token")
        })

        StepVerifier.create(guestService.getGuestId(request))
            .expectNext("mocked-guest-id")
            .verifyComplete()
        verify(request, times(0)).headers
        // Verify that guestService does not continue to find guestId from header
        verify(jwtService, times(0)).validateAndExtractGuestId("token")
    }

    @Test
    fun `getGuestId should return guestId from Authorization header if guestId not presented in cookie`() {
        val cookies = LinkedMultiValueMap<String, HttpCookie>()
        `when`(request.cookies).thenReturn(cookies)
        `when`(request.headers).thenReturn(HttpHeaders().apply {
            add(HttpHeaders.AUTHORIZATION, "Bearer token")
        })
        `when`(jwtService.validateAndExtractGuestId("token"))
            .thenReturn(Mono.just("guest-id"))

        StepVerifier.create(guestService.getGuestId(request))
            .expectNext("guest-id")
            .verifyComplete()

        verify(jwtService).validateAndExtractGuestId("token")
    }

    @Test
    fun `getGuestId should return empty when no guestId`() {
        val cookies = LinkedMultiValueMap<String, HttpCookie>()
        `when`(request.cookies).thenReturn(cookies)
        `when`(request.headers).thenReturn(HttpHeaders())

        StepVerifier.create(guestService.getGuestId(request))
            .verifyComplete()
    }

    @Test
    fun `generateNewGuestId should create guestId and set cookies and headers`() {
        val headers = HttpHeaders()
        `when`(response.headers).thenReturn(headers)

        StepVerifier.create(guestService.generateNewGuestId(response))
            .expectNextMatches { guestId ->
                guestId.isNotEmpty()
            }
            .verifyComplete()

        verify(response).addCookie(any(ResponseCookie::class.java))
        assertNotNull(headers.getFirst(HttpHeaders.AUTHORIZATION))
    }
}
