package com.goodfeel.nightgrass.service

import com.goodfeel.nightgrass.data.User
import com.goodfeel.nightgrass.dto.UserDto
import com.goodfeel.nightgrass.repo.UserRepository
import com.goodfeel.nightgrass.web.util.Utility
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class UserService(private val userRepository: UserRepository) {

    fun findUserById(oauthId: String): Mono<UserDto> {
        return userRepository.findByOauthId(oauthId).map { user: User ->
            val userDto = UserDto(
                customerName = user.customerName,
                phone = user.phone,
                address = user.address
            )
            userDto
        }
    }

    fun updateUserInfo(userDto: UserDto): Mono<User> {
        return Utility.currentUserId.flatMap { oauthId: String ->
            userRepository.findByOauthId(oauthId)
        }.flatMap { user: User ->
            val updatedUser = user.copy(
                customerName = userDto.customerName,
                phone = userDto.phone,
                address = userDto.address
            )
            userRepository.save(updatedUser)
        }
    }
}
