package com.rs.payments.wallet.service.impl;

import com.rs.payments.wallet.exception.DuplicateResourceException;
import com.rs.payments.wallet.model.User;
import com.rs.payments.wallet.repository.UserRepository;
import com.rs.payments.wallet.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User createUser(User user) {

        // Check duplicate username
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }

        // Check duplicate email
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        return userRepository.save(user);
    }
}