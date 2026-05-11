package com.example.staff_service.controller;

import com.example.staff_service.dto.StaffDto;
import com.example.staff_service.dto.StaffRequest;
import com.example.staff_service.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/staff")
@RequiredArgsConstructor
@Slf4j
public class StaffController {
    private final StaffService service;

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody @Valid StaffRequest request) {
        log.info("Adding new staff: {}", request.getName());
        try {
            return ResponseEntity.ok(service.addStaff(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<StaffDto> update(@PathVariable Long id, @RequestBody @Valid StaffRequest request) {
        log.info("Updating staff with ID {}: {}", id, request.getName());
        return ResponseEntity.ok(service.updateStaff(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.warn("Deleting staff with ID {}", id);
        service.deleteStaff(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<StaffDto> getById(@PathVariable Long id) {
        log.debug("Fetching staff by ID: {}", id);
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/allStaff")
    public ResponseEntity<List<StaffDto>> getAll() {
        log.debug("Fetching all staff members");
        return ResponseEntity.ok(service.getAllStaff());
    }
}
