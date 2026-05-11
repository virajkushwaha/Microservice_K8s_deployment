package com.example.reservation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String to;
    private String subject;
    private String body;

    private byte[] attachment;
    private String attachmentName;
}
