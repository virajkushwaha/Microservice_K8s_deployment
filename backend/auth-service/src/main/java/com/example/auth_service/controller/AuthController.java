package com.example.auth_service.controller;

import com.example.auth_service.dto.AuthRequest;
import com.example.auth_service.dto.PasswordUpdateRequest;
import com.example.auth_service.dto.RegisterRequest;
import com.example.auth_service.model.Role;
import com.example.auth_service.model.User;
import com.example.auth_service.config.JwtUtil;
import com.example.auth_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class
AuthController {
    private final UserService userService;
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid RegisterRequest req) {
        log.info("Registering user with role: {}", req.getRole());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentRole = auth.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("");

        Role newUserRole = req.getRole();

        if (currentRole.equals("ROLE_OWNER") && (newUserRole == Role.MANAGER || newUserRole == Role.RECEPTIONIST)) {
            // allowed
        } else if (currentRole.equals("ROLE_MANAGER") && newUserRole == Role.RECEPTIONIST) {
            // allowed
        } else {
            log.warn("Unauthorized role registration attempt: current={}, requested={}", currentRole, newUserRole);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied: insufficient permissions");
        }

        userService.save(new User(null, req.getUsername(), req.getPassword(), req.getEmail(), newUserRole));
        log.info("User registered successfully: {}", req.getUsername());
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody @Valid AuthRequest req) {
        log.info("Login attempt for user: {}", req.getUsername());

        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );

        User user = userService.findByUserName(req.getUsername());
        String token = jwtUtil.generateToken(user);
        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put("token", token);

        log.info("Login successful for user: {}", req.getUsername());
        return ResponseEntity.ok(tokenMap);
    }

    @PostMapping("/register-owner")
    public ResponseEntity<String> registerOwner(@RequestBody @Valid RegisterRequest req) {
        log.info("Registering owner account: {}", req.getUsername());
        if (!req.getRole().equals(Role.OWNER)) {
            log.warn("Owner registration attempt failed. Invalid role: {}", req.getRole());
            return ResponseEntity.badRequest().body("Only role OWNER allowed here");
        }

        userService.save(new User(null, req.getUsername(), req.getPassword(), req.getEmail(), req.getRole()));
        return ResponseEntity.ok("Owner registered");
    }

    @DeleteMapping("/delete/{username}")
    public ResponseEntity<String> deleteUser(@PathVariable String username) {
        log.info("Delete request for user: {}", username);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentRole = auth.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("");

        User userToDelete = userService.findByUserName(username);
        Role targetRole = userToDelete.getRole();

        if (currentRole.equals("ROLE_OWNER")) {
            // allowed
        } else if (currentRole.equals("ROLE_MANAGER") &&
                (targetRole == Role.MANAGER || targetRole == Role.RECEPTIONIST)) {
            // allowed
        } else {
            log.warn("Unauthorized delete attempt by {} for {}", currentRole, targetRole);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied: insufficient permissions");
        }

        userService.deleteByUsername(username);
        log.info("User deleted: {}", username);
        return ResponseEntity.ok("User deleted successfully");
    }

    @PutMapping("/update-username/{username}")
    public ResponseEntity<String> updateUsername(@PathVariable String username, @RequestBody @Valid RegisterRequest req) {
        log.info("Updating username for: {}", username);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!auth.getName().equals(username)) {
            log.warn("Unauthorized username update attempt by {}", auth.getName());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only update your own username");
        }

        User existingUser = userService.findByUserName(username);
        existingUser.setUsername(req.getUsername());
        userService.save(existingUser);
        return ResponseEntity.ok("Username updated successfully");
    }

    @PutMapping("/update-password/{username}")
    public ResponseEntity<String> updatePassword(@PathVariable String username, @RequestBody @Valid PasswordUpdateRequest newPassword) {
        log.info("Updating password for: {}", username);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!auth.getName().equals(username)) {
            log.warn("Unauthorized password update attempt by {}", auth.getName());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only update your own password");
        }

        User existingUser = userService.findByUserName(username);
        userService.updatePassword(username, newPassword.getPassword());
        return ResponseEntity.ok("Password updated successfully");
    }
}
