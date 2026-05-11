package com.example.reservation_service.dto;

import com.example.reservation_service.validation.ValidRoomType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReservationRequest {

    @NotBlank(message = "Guest name is required")
    @Size(min = 3, message = "Guest name must be at least 3 characters")
    private String guestName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "\\d{10}", message = "Phone number must be 10 digits")
    private String phoneNumber;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String emailId;

    @NotBlank(message = "Room type is required")
    @ValidRoomType
    private String roomType;


    @NotNull(message = "Check-in date is required")
    @FutureOrPresent(message = "Check-in date must be today or in the future")
    private LocalDate checkIn;

    @NotNull(message = "Check-out date is required")
    @Future(message = "Check-out date must be in the future")
    private LocalDate checkOut;

    @Min(value = 0, message = "Number of children cannot be negative")
    private int numChildren;

    @Min(value = 1, message = "At least one adult is required")
    private int numAdults;
}
