package com.example.staff_service.mapper;

import com.example.staff_service.dto.StaffDto;
import com.example.staff_service.dto.StaffRequest;
import com.example.staff_service.model.Staff;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class StaffMapper {
    public StaffDto toDto(Staff staff) {
        StaffDto dto = new StaffDto();
        BeanUtils.copyProperties(staff, dto);
        return dto;
    }

    public Staff toEntity(StaffRequest req) {
        Staff staff = new Staff();
        BeanUtils.copyProperties(req, staff);
        return staff;
    }
}