package com.example.staff_service;

import com.example.staff_service.controller.StaffController;
import com.example.staff_service.dto.StaffDto;
import com.example.staff_service.dto.StaffRequest;
import com.example.staff_service.service.StaffService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class StaffControllerTest {

    @InjectMocks
    private StaffController controller;

    @Mock
    private StaffService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAddStaff_Success() {
        StaffRequest request = new StaffRequest();
        StaffDto dto = new StaffDto();

        when(service.addStaff(request)).thenReturn(dto);

        ResponseEntity<?> response = controller.add(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto, response.getBody());
    }

    @Test
    void testAddStaff_Conflict() {
        StaffRequest request = new StaffRequest();
        String errorMessage = "Staff already exists";

        when(service.addStaff(request)).thenThrow(new IllegalArgumentException(errorMessage));

        ResponseEntity<?> response = controller.add(request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(errorMessage, response.getBody());
    }


    @Test
    void testUpdateStaff() {
        Long id = 1L;
        StaffRequest request = new StaffRequest();
        StaffDto dto = new StaffDto();
        when(service.updateStaff(id, request)).thenReturn(dto);

        ResponseEntity<StaffDto> response = controller.update(id, request);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(dto, response.getBody());
    }

    @Test
    void testDeleteStaff() {
        Long id = 1L;
        doNothing().when(service).deleteStaff(id);

        ResponseEntity<Void> response = controller.delete(id);
        assertEquals(204, response.getStatusCodeValue());
        verify(service, times(1)).deleteStaff(id);
    }

    @Test
    void testGetById() {
        Long id = 1L;
        StaffDto dto = new StaffDto();
        when(service.getById(id)).thenReturn(dto);

        ResponseEntity<StaffDto> response = controller.getById(id);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(dto, response.getBody());
    }

    @Test
    void testGetAllStaff() {
        List<StaffDto> staffList = Arrays.asList(new StaffDto(), new StaffDto());
        when(service.getAllStaff()).thenReturn(staffList);

        ResponseEntity<List<StaffDto>> response = controller.getAll();
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(staffList, response.getBody());
    }
}
