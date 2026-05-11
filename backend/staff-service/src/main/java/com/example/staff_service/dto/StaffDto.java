package com.example.staff_service.dto;

import lombok.Data;

@Data
public class StaffDto {
    private Long id;
    private String employeeCode;
    private String name;
    private String address;
    private Double salary;
    private Integer age;
    private String occupation;
    private String email;
}
