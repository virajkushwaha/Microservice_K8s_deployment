package com.example.staff_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import jakarta.persistence.PrePersist;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Staff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String employeeCode;
    private String name;
    private String address;
    private Double salary;
    private Integer age;
    private String occupation;
    private String email;

    @PrePersist
    public void generateEmployeeCode() {
        this.employeeCode = "EMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
}