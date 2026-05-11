package com.example.staff_service;

import com.example.staff_service.dto.StaffDto;
import com.example.staff_service.dto.StaffRequest;
import com.example.staff_service.mapper.StaffMapper;
import com.example.staff_service.model.Staff;
import com.example.staff_service.repository.StaffRepository;
import com.example.staff_service.service.StaffService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class StaffServiceTest {

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private StaffMapper staffMapper;

    @InjectMocks
    private StaffService staffService;

    private StaffRequest request;
    private Staff staff;
    private StaffDto staffDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        request = new StaffRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setAddress("123 Main St");
        request.setAge(30);
        request.setSalary(50000.0);
        request.setOccupation("Receptionist");

        staff = new Staff();
        staff.setId(1L);
        staff.setName(request.getName());
        staff.setEmail(request.getEmail());

        staffDto = new StaffDto();
        staffDto.setId(1L);
        staffDto.setName(request.getName());
        staffDto.setEmail(request.getEmail());
    }

    @Test
    void testAddStaff() {
        when(staffMapper.toEntity(request)).thenReturn(staff);
        when(staffRepository.save(staff)).thenReturn(staff);
        when(staffMapper.toDto(staff)).thenReturn(staffDto);

        StaffDto result = staffService.addStaff(request);
        assertEquals(staffDto.getName(), result.getName());
        verify(staffRepository).save(staff);
    }

    @Test
    void testUpdateStaff() {
        when(staffRepository.findById(1L)).thenReturn(Optional.of(staff));
        when(staffRepository.save(any())).thenReturn(staff);
        when(staffMapper.toDto(any())).thenReturn(staffDto);

        StaffDto result = staffService.updateStaff(1L, request);
        assertEquals(staffDto.getName(), result.getName());
        verify(staffRepository).save(any());
    }

    @Test
    void testDeleteStaff() {
        Long id = 1L;
        when(staffRepository.existsById(id)).thenReturn(true);
        staffService.deleteStaff(id);
        verify(staffRepository, times(1)).deleteById(id);
    }



    @Test
    void testGetById() {
        when(staffRepository.findById(1L)).thenReturn(Optional.of(staff));
        when(staffMapper.toDto(staff)).thenReturn(staffDto);

        StaffDto result = staffService.getById(1L);
        assertNotNull(result);
        assertEquals("John Doe", result.getName());
    }

    @Test
    void testGetAllStaff() {
        when(staffRepository.findAll()).thenReturn(List.of(staff));
        when(staffMapper.toDto(staff)).thenReturn(staffDto);

        List<StaffDto> result = staffService.getAllStaff();
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getName());
    }
}
