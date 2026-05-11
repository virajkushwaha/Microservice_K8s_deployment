package com.example.auth_service.service;

import com.example.auth_service.model.User;
import com.example.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepo;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public UserDetails loadUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        if (user.getRole() == null) {
            throw new IllegalStateException("User role is not defined");
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Arrays.asList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }

    public User save(User user) {
        if (user == null) throw new IllegalArgumentException("User must not be null");

        if (userRepo.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("User with username already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepo.save(user);
    }

    public User findByUserName(String username) {
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    public void deleteByUsername(String username) {
        Optional<User> userOpt = userRepo.findByUsername(username);
        userOpt.ifPresent(userRepo::delete);
    }

    public User updatePassword(String username, String rawPassword) {
        User user = findByUserName(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        return userRepo.save(user);
    }
}
