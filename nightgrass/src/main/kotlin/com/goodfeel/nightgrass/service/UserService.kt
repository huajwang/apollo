package com.goodfeel.nightgrass.service

import com.goodfeel.nightgrass.data.User
import com.goodfeel.nightgrass.repo.UserRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class UserService(private val userRepository: UserRepository) {

    fun updateUserInfo(user: User): Mono<User> = userRepository.save(user)
    fun deleteUserByGuestId(guestId: String): Mono<Void> =
        userRepository.deleteByGuestId(guestId)

}
