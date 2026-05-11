package com.example.staff_service.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class StaffRequest {


    @NotBlank(message = "Name must not be blank")
    @Size(min = 2, max = 50, message = "Name must be between 2 to 50 characters")
    @Pattern(regexp = "^[A-Za-z ]+$", message = "Name must contain only letters and spaces")
    private String name;

    @NotBlank(message = "Address must not be blank")
    @Size(min = 5, max = 100, message = "Address must be between 5 to 100 characters")
    private String address;

    @NotNull(message = "Salary must not be null")
    @Min(value = 0, message = "Salary must be non-negative")
    private Double salary;

    @NotNull(message = "Age must not be null")
    @Min(value = 18, message = "Age must be at least 18")
    private Integer age;

    @NotBlank(message = "Occupation must not be blank")
    @Size(min = 3, max = 30, message = "Occupation must be between 3 to 30 characters")
    @Pattern(regexp = "^[A-Za-z ]+$", message = "Occupation must contain only letters and spaces")
    private String occupation;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email format is invalid")
    private String email;
}

