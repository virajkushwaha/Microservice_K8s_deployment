package com.example.reservation_service.validation;

import com.example.room_service.model.RoomType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;

public class RoomTypeValidator implements ConstraintValidator<ValidRoomType, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return false;

        return Arrays.stream(RoomType.values())
                .anyMatch(e -> e.name().equalsIgnoreCase(value.trim()));
    }
}
