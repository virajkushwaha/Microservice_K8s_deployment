package com.example.staff_service.service;

import com.example.staff_service.exception.ResourceNotFoundException;
import com.example.staff_service.dto.StaffDto;
import com.example.staff_service.dto.StaffRequest;
import com.example.staff_service.mapper.StaffMapper;
import com.example.staff_service.model.Staff;
import com.example.staff_service.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository repository;
    private final StaffMapper mapper;

    public StaffDto addStaff(StaffRequest request) {
        Optional<Staff> existing = repository.findByEmail(request.getEmail());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Staff already exists with email: " + request.getEmail());
        }

        Staff staff = mapper.toEntity(request);
        return mapper.toDto(repository.save(staff));
    }

    public StaffDto updateStaff(Long id, StaffRequest request) {
        Staff existingStaff = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff with ID " + id + " not found"));

        BeanUtils.copyProperties(request, existingStaff, "id");
        return mapper.toDto(repository.save(existingStaff));
    }

    public void deleteStaff(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Staff with ID " + id + " not found");
        }
        repository.deleteById(id);
    }

    public StaffDto getById(Long id) {
        Staff staff = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff with ID " + id + " not found"));
        return mapper.toDto(staff);
    }

    public List<StaffDto> getAllStaff() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }
}
