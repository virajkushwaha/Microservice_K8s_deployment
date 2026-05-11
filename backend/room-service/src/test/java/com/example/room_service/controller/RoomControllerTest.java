package com.example.room_service.controller;

import com.example.room_service.dto.RoomDto;
import com.example.room_service.dto.RoomRequest;
import com.example.room_service.model.RoomStatus;
import com.example.room_service.model.RoomType;
import com.example.room_service.service.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomController.class)
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoomService service;

    @Autowired
    private ObjectMapper objectMapper;

    private RoomRequest request;
    private RoomDto dto;

    @BeforeEach
    void setUp() {
        request = new RoomRequest();
        request.setRoomNumber("101");
        request.setRoomType(RoomType.DELUXE);
        request.setRatePerNight(1000);
        request.setCapacity(2);
        request.setStatus(RoomStatus.AVAILABLE);

        dto = new RoomDto();
        dto.setId(1L);
        dto.setRoomNumber("101");
    }

    @Test
    void create_shouldReturnRoomDto() throws Exception {
        when(service.addRoom(any())).thenReturn(dto);

        mockMvc.perform(post("/room/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void all_shouldReturnRoomList() throws Exception {
        when(service.getAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/room/allRooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void update_shouldReturnUpdatedDto() throws Exception {
        when(service.updateRoom(eq(1L), any())).thenReturn(dto);

        mockMvc.perform(put("/room/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateStatus_shouldReturnNoContent() throws Exception {
        mockMvc.perform(put("/room/101/status")  // ✅ "101" as String
                        .param("status", "OCCUPIED"))
                .andExpect(status().isNoContent());

        verify(service).updateRoomStatus("101", RoomStatus.OCCUPIED);  // ✅ roomNumber as String
    }


    @Test
    void search_shouldReturnAvailableRooms() throws Exception {
        when(service.searchAvailableRooms(2)).thenReturn(List.of(dto));

        mockMvc.perform(get("/room/available").param("guests", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
