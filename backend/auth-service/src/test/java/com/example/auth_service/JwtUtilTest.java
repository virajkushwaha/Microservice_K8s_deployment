package com.example.auth_service;

import com.example.auth_service.config.JwtUtil;
import com.example.auth_service.model.Role;
import com.example.auth_service.model.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;


import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
    }

    @Test
    void testGenerateTokenAndExtractClaims() {
        User appUser = new User(
                1L, "john", "password123", "john@email.com", Role.MANAGER);

        String token = jwtUtil.generateToken(appUser);
        assertNotNull(token);

        Claims claims = jwtUtil.extractAllClaims(token);
        assertEquals("john", claims.getSubject());
        assertEquals("MANAGER", claims.get("role"));
    }

    @Test
    void testIsTokenValid_ValidToken_ReturnsTrue() {
        User appUser = new User(
                1L, "john", "password123", "john@email.com", Role.MANAGER);

        String token = jwtUtil.generateToken(appUser);

        // ✅ Correct import from Spring Security
        UserDetails springUser = new org.springframework.security.core.userdetails.User(
                "john", "password123", Collections.emptyList());

        assertTrue(jwtUtil.isTokenValid(token, springUser));
    }

    @Test
    void testIsTokenValid_InvalidUsername_ReturnsFalse() {
        User appUser = new User(
                1L, "john", "password123", "john@email.com", Role.MANAGER);

        String token = jwtUtil.generateToken(appUser);

        UserDetails differentUser = new org.springframework.security.core.userdetails.User(
                "jane", "password123", Collections.emptyList());

        assertFalse(jwtUtil.isTokenValid(token, differentUser));
    }
}
