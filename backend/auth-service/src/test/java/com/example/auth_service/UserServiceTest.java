package com.example.auth_service;



import com.example.auth_service.model.Role;
import com.example.auth_service.model.User;
import com.example.auth_service.repository.UserRepository;
import com.example.auth_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSave_ValidUser_ShouldSaveSuccessfully() {
        User user = new User(null, "john", "password123", "john@email.com", Role.MANAGER);
        when(userRepository.findByUsername("john")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);

        User savedUser = userService.save(user);

        assertNotNull(savedUser);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testSave_UserAlreadyExists_ShouldThrowException() {
        User user = new User(null, "john", "password123", "john@email.com", Role.MANAGER);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> userService.save(user));
        assertEquals("User with username already exists", ex.getMessage());
    }

    @Test
    void testFindByUserName_NotFound_ShouldThrowException() {
        when(userRepository.findByUsername("no_user")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> userService.findByUserName("no_user"));
    }
}
