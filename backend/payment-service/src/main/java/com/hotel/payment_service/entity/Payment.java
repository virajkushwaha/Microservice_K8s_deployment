package com.hotel.payment_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Double amount;
    private String paymentMethod; // CARD, UPI, etc.
    private String status; // SUCCESS, FAILED
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private LocalDateTime paymentTime;
}


//package com.hotel.payment_service.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class Payment {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String paymentMethod; // Stripe, etc.
//    private String stripePaymentIntentId; // Unique ID for Stripe PaymentIntent
//    private Double amount;
//    private String currency;
//    private String status; // CREATED, SUCCESS, FAILED
//    private LocalDateTime paymentTime;
//}