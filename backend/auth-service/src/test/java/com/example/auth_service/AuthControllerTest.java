package com.example.auth_service;

import com.example.auth_service.config.JwtUtil;
import com.example.auth_service.controller.AuthController;
import com.example.auth_service.dto.AuthRequest;
import com.example.auth_service.dto.RegisterRequest;
import com.example.auth_service.model.Role;
import com.example.auth_service.model.User;
import com.example.auth_service.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthenticationManager authManager;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContextWithRole(String role) {
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
        Authentication auth = new UsernamePasswordAuthenticationToken("user", "password", List.of(authority));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    void testLogin_Success() throws Exception {
        AuthRequest request = new AuthRequest("john", "password123");

        User user = new User(1L, "john", "encoded_pass", "john@email.com", Role.MANAGER);
        Authentication auth = Mockito.mock(Authentication.class);

        Mockito.when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        Mockito.when(userService.findByUserName("john")).thenReturn(user);
        Mockito.when(jwtUtil.generateToken(user)).thenReturn("dummy-token");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("dummy-token"));
    }

    @Test
    void testRegisterOwner_InvalidRole_ShouldFail() throws Exception {
        RegisterRequest req = new RegisterRequest("owner", "pass123", "owner@email.com", Role.MANAGER);

        mockMvc.perform(post("/auth/register-owner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Only role OWNER allowed here"));
    }

    @Test
    void testRegister_ByOwner_ForManager_ShouldPass() throws Exception {
        setSecurityContextWithRole("OWNER");

        RegisterRequest req = new RegisterRequest("manager1", "pass123", "m1@email.com", Role.MANAGER);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));
    }

    @Test
    void testRegister_ByOwner_ForReceptionist_ShouldPass() throws Exception {
        setSecurityContextWithRole("OWNER");

        RegisterRequest req = new RegisterRequest("reception1", "pass123", "r1@email.com", Role.RECEPTIONIST);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));
    }

    @Test
    void testRegister_ByManager_ForReceptionist_ShouldPass() throws Exception {
        setSecurityContextWithRole("MANAGER");

        RegisterRequest req = new RegisterRequest("reception2", "pass123", "r2@email.com", Role.RECEPTIONIST);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));
    }

    @Test
    void testRegister_ByManager_ForManager_ShouldFail() throws Exception {
        setSecurityContextWithRole("MANAGER");

        RegisterRequest req = new RegisterRequest("manager2", "pass123", "m2@email.com", Role.MANAGER);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Access denied: insufficient permissions"));
    }

    @Test
    void testRegister_ByManager_ForOwner_ShouldFail() throws Exception {
        setSecurityContextWithRole("MANAGER");

        RegisterRequest req = new RegisterRequest("owner2", "pass123", "o2@email.com", Role.OWNER);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Access denied: insufficient permissions"));
    }
}