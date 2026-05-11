package com.example.staff_service.repository;

import com.example.staff_service.model.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {
    Optional<Staff> findByEmployeeCode(String employeeCode);

    Optional<Staff> findByEmail(String email);

}
