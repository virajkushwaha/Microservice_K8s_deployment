package com.example.reservation_service.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = RoomTypeValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRoomType {
    String message() default "Invalid room type. Allowed values are: SINGLE, DOUBLE, SUITE, etc.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
