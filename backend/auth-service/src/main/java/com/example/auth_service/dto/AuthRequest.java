package com.example.auth_service.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

    @NotBlank(message = "Username is required")
    @Pattern(
            regexp = "^[A-Za-z]{3,}[A-Za-z0-9@#$%^&+=_.-]*$",
            message = "Username must start with at least 3 letters and may contain letters, numbers, or special characters (no spaces)"
    )
    private String username;


    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Password must be at least 8 characters and contain uppercase, lowercase, number, and special character"
    )
    private String password;
}
