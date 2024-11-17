package com.goodfeel.nightgrass.service;

import com.goodfeel.nightgrass.data.User;
import com.goodfeel.nightgrass.dto.UserDto;
import com.goodfeel.nightgrass.repo.UserRepository;
import com.goodfeel.nightgrass.web.util.Utility;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserService {

    private final UserRepository userRepository;


    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Mono<UserDto> findUserById(String oauthId) {
        return userRepository.findByOauthId(oauthId).map(user -> {
            UserDto userDto = new UserDto();
            userDto.setCustomerName(user.getCustomerName());
            userDto.setPhone(user.getPhone());
            userDto.setAddress(user.getAddress());
            return userDto;
        });
    }

    public Mono<User> saveUser(UserDto userDto) {
        return Utility.getCurrentUserId().flatMap(userRepository::findByOauthId).flatMap(user -> {
            user.setCustomerName(userDto.getCustomerName());
            user.setPhone(userDto.getPhone());
            user.setAddress(userDto.getAddress());
            return userRepository.save(user);
        });
    }

}

