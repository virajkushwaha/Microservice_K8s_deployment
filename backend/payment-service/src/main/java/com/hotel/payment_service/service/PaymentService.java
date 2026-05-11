package com.hotel.payment_service.service;

import java.util.Map;

public interface PaymentService {
    Map<String, String> createPaymentOrder(int amount);
    boolean confirmPayment(String orderId, String paymentId, String razorpaySign);
}


//package com.hotel.payment_service.service;
//
//import java.util.Map;
//
//public interface PaymentService {
//    Map<String, String> createPaymentOrder(int amount);
//    boolean confirmPayment(String payload, String stripeSignature);
//}