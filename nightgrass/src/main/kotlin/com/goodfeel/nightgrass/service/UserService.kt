package com.goodfeel.nightgrass.service

import com.goodfeel.nightgrass.data.User
import com.goodfeel.nightgrass.data.Address
import com.goodfeel.nightgrass.repo.UserRepository
import com.goodfeel.nightgrass.repo.AddressRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class UserService(
    private val userRepository: UserRepository,
    private val addressRepository: AddressRepository
) {

    private val logger = LoggerFactory.getLogger(UserService::class.java)
    /**
     * Save or update user profile during OAuth2 authentication.
     * If user exists (by oauthId), update their profile data (name, email, avatar).
     * If user is new, create and save them to the database.
     *
     * This ensures user data is persisted for orders, cart, reviews, and user management.
     *
     * @param oauthId Unique identifier from OAuth2 provider (sub, id, user_id)
     * @param nickName User display name from OAuth2 provider
     * @param email User email from OAuth2 provider
     * @param avatar User avatar URL from OAuth2 provider
     * @return Mono<User> with the saved/updated user
     */
    fun saveOrUpdateOAuth2User(
        oauthId: String,
        nickName: String,
        email: String,
        avatar: String
    ): Mono<User> {
        return userRepository.findByOauthId(oauthId)
            .flatMap { existingUser ->
                // User exists - update profile data
                logger.debug("Updating existing OAuth2 user: $oauthId")
                val updatedUser = existingUser.copy(
                    nickName = nickName.takeIf { it.isNotBlank() } ?: existingUser.nickName,
                    email = email.takeIf { it.isNotBlank() } ?: existingUser.email,
                    avatar = avatar.takeIf { it.isNotBlank() } ?: existingUser.avatar
                )
                userRepository.save(updatedUser)
            }
            .switchIfEmpty(
                // User doesn't exist - create new user
                Mono.defer {
                    logger.debug("Creating new OAuth2 user: $oauthId")
                    val newUser = User(
                        oauthId = oauthId,
                        nickName = nickName,
                        email = email,
                        avatar = avatar
                    )
                    userRepository.save(newUser)
                }
            )
            .doOnSuccess { user ->
                logger.info("OAuth2 user saved/updated successfully: ${user.oauthId}")
            }
            .onErrorResume { error ->
                logger.error("Failed to save/update OAuth2 user: $oauthId", error)
                Mono.error(error)
            }
    }

    fun getUserByOauthId(oauthId: String): Mono<User> {
        return userRepository.findByOauthId(oauthId)
    }

    fun updateAddress(
        oauthId: String,
        customerName: String,
        phone: String,
        address: String,
        city: String,
        postalCode: String
    ): Mono<Address> {
        return userRepository.findByOauthId(oauthId)
            .flatMap { user ->
                // For now, we'll just create a new address or update the default one if it exists
                // This logic can be expanded to support multiple addresses
                addressRepository.findByUserIdAndIsDefaultTrue(user.id!!)
                    .flatMap { existingAddress ->
                        val updatedAddress = existingAddress.copy(
                            customerName = customerName,
                            phone = phone,
                            addressLine = address,
                            city = city,
                            postalCode = postalCode
                        )
                        addressRepository.save(updatedAddress)
                    }
                    .switchIfEmpty(
                        Mono.defer {
                            val newAddress = Address(
                                userId = user.id,
                                customerName = customerName,
                                phone = phone,
                                addressLine = address,
                                city = city,
                                postalCode = postalCode,
                                isDefault = true
                            )
                            addressRepository.save(newAddress)
                        }
                    )
            }
    }
}
