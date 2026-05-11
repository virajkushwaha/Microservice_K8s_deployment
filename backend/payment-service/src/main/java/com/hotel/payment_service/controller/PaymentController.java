package com.hotel.payment_service.controller;

import com.hotel.payment_service.dto.RazorPaymentResponse;
import com.hotel.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "http://127.0.0.1:5500")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createOrder(@Valid @RequestParam int amount) {
        return ResponseEntity.ok(paymentService.createPaymentOrder(amount));
    }

//    @PostMapping("/confirm")
//    public ResponseEntity<?> verifyPayment(@RequestParam String orderId, @RequestParam String paymentId, @RequestParam String razorpaySign){
//        try{
//            boolean isValid = paymentService.confirmPayment(orderId, paymentId, razorpaySign);
//            return (isValid) ? ResponseEntity.ok("Payment verify SuccessFully") : ResponseEntity.status(400).body("Payment verification failed");
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body("Error while payment verify");
//        }
//    }

@PostMapping("/confirm")
public ResponseEntity<?> verifyPayment(@RequestBody RazorPaymentResponse dto){
    try{
        boolean isValid = paymentService.confirmPayment(
                dto.getRazorpayOrderId(),
                dto.getRazorpayPaymentId(),
                dto.getRazorpaySignature()
        );
        return (isValid) ? ResponseEntity.ok("Payment verify SuccessFully") : ResponseEntity.status(400).body("Payment verification failed");
    } catch (Exception e) {
        return ResponseEntity.status(500).body("Error while payment verify");
    }
}
}

//    @PostMapping("/confirm")
//    public ResponseEntity<?> verifyPayment(@RequestBody String payload, @RequestHeader("Stripe-Signature") String stripeSignature) {
//        try {
//            boolean isValid = paymentService.confirmPayment(payload, stripeSignature);
//            return isValid
//                    ? ResponseEntity.ok("Payment verified successfully")
//                    : ResponseEntity.status(400).body("Payment verification failed");
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body("Error while verifying payment");
//        }
//    }
//}
