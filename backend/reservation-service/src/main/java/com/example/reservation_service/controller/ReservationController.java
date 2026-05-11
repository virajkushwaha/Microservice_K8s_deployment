package com.example.reservation_service.controller;

import com.example.reservation_service.dto.ReservationDto;
import com.example.reservation_service.dto.ReservationRequest;
import com.example.reservation_service.model.Reservation;
import com.example.reservation_service.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
@Slf4j
public class ReservationController {
    private final ReservationService service;

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody @Valid ReservationRequest req, BindingResult bindingResult) {
        log.info("Received reservation creation request: {}", req);

        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            for (FieldError error : bindingResult.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
            log.warn("Validation failed for reservation request: {}", errors);
            return ResponseEntity.badRequest().body(errors);
        }

        return ResponseEntity.ok(service.createReservation(req));
    }

    @GetMapping
    public ResponseEntity<List<ReservationDto>> getAll() {
        log.info("Fetching all reservations");
        return ResponseEntity.ok(service.getAll());
    }

    @PutMapping("/cancel/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        log.info("Request to cancel reservation ID: {}", id);
        service.cancelReservation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<Reservation>> searchReservations(
            @RequestParam String guestName,
            @RequestParam String phoneNumber) {

        List<Reservation> reservations = service.searchReservations(guestName, phoneNumber);

        if (reservations.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(reservations);
    }

    @PutMapping("/checkout")
    public ResponseEntity<Void> checkoutByReservationCode(@RequestParam String reservationCode) {
        log.info("Checkout request received for reservation code: {}", reservationCode);
        service.checkoutByReservationCode(reservationCode);
        return ResponseEntity.noContent().build();
    }
}
