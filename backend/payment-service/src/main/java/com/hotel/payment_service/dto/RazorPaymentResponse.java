package com.hotel.payment_service.dto;

import lombok.Data;

@Data
public class RazorPaymentResponse {
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
}


//package com.hotel.payment_service.dto;
//
//import lombok.Data;
//
//@Data
//public class StripePaymentResponse {
//    private String paymentIntentId;
//    private String clientSecret;
//    private double amount;
//    private String currency;
//    private String status;
//    private String payload;
//    private String stripeSignature;
//}