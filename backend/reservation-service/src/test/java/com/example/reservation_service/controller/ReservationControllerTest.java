package com.example.reservation_service.controller;

import com.example.reservation_service.dto.ReservationDto;
import com.example.reservation_service.dto.ReservationRequest;
import com.example.reservation_service.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.*;

@WebMvcTest(ReservationController.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReservationService service;

    @Test
    void testCreateReservation() throws Exception {
        // Prepare JSON manually with ISO date format
        String requestJson = """
        {
            "guestName": "John Doe",
            "phoneNumber": "9876543210",
            "emailId": "john@example.com",
            "checkIn": "2025-06-20",
            "checkOut": "2025-06-25",
            "numChildren": 1,
            "numAdults": 2
        }
    """;

        ReservationDto responseDto = new ReservationDto();
        responseDto.setGuestName("John Doe");
        // set other fields if needed

        Mockito.when(service.createReservation(Mockito.any())).thenReturn(responseDto);

        mockMvc.perform(post("/reservations/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAllReservations() throws Exception {
        ReservationDto dto = new ReservationDto();
        dto.setId(1L);
        Mockito.when(service.getAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void testCancelReservation() throws Exception {
        Long id = 1L;

        mockMvc.perform(put("/reservations/cancel/{id}", id))
                .andExpect(status().isNoContent());

        verify(service).cancelReservation(id);
    }

    @Test
    void testCheckoutByReservationCode_success() throws Exception {
        String reservationCode = "RSV_12345678";

        mockMvc.perform(put("/checkout")
                        .param("reservationCode", reservationCode))
                .andExpect(status().isNoContent());

        verify(service).checkoutByReservationCode(reservationCode);
    }

    @Test
    void testCheckoutByReservationCode_missingParam() throws Exception {
        mockMvc.perform(put("/checkout"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCheckoutByReservationCode_serviceThrowsException() throws Exception {
        String reservationCode = "INVALID_CODE";

        doThrow(new RuntimeException("No active reservation found for this code"))
                .when(service).checkoutByReservationCode(reservationCode);

        mockMvc.perform(put("/checkout")
                        .param("reservationCode", reservationCode))
                .andExpect(status().isInternalServerError());
    }

}
