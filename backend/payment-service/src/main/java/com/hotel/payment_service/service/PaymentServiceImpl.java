package com.hotel.payment_service.service;

import com.hotel.payment_service.config.RazorpayConfig;
import com.hotel.payment_service.entity.Payment;
import com.hotel.payment_service.repository.PaymentRepository;
import com.hotel.payment_service.utils.Util;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepository;
    final RazorpayConfig razorpayConfig;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String secret;

    @Override
    public Map<String, String> createPaymentOrder(int amount){
        int paybleamount = amount;
        try {
            JSONObject options = new JSONObject();
            options.put("amount", paybleamount);
            options.put("currency", "INR");
            options.put("payment_capture",1);
            options.put("receipt", UUID.randomUUID().toString());

            Order order = razorpayClient.orders.create(options);

            Payment payment = Payment.builder()
                    .paymentMethod("RAZORPAY")
                    .razorpayOrderId(order.get("id"))
                    .amount((double) paybleamount/100.0)
                    .status("CREATED")
                    .paymentTime(LocalDateTime.now())
                    .build();

            paymentRepository.save(payment);

            Map<String, String> response = new HashMap<>();
            response.put("orderId", order.get("id"));
            response.put("key", razorpayKeyId);
            response.put("amount", String.valueOf(paybleamount));
            response.put("currency", "INR");
            return response;

        } catch (RazorpayException e) {
            throw new RuntimeException("Failed to create order in Razorpay", e);
        }
    }

  @Override
  public boolean confirmPayment(String orderId, String paymentId, String razorpaySign) {
    String payload = orderId + '|' + paymentId;
    try {
        boolean valid = Util.verifySign(payload, razorpaySign, razorpayConfig.getKeySecret());
        Payment payment = paymentRepository.findByRazorpayOrderId(orderId).orElseThrow();
        payment.setRazorpayPaymentId(paymentId);
        payment.setStatus(valid ? "SUCCESS" : "FAILED");
        paymentRepository.save(payment);
        return valid;
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}
}



//package com.hotel.payment_service.service;
//
//import com.hotel.payment_service.config.StripeConfig;
//import com.hotel.payment_service.entity.Payment;
//import com.hotel.payment_service.repository.PaymentRepository;
//import com.stripe.model.EventDataObjectDeserializer;
//import com.stripe.model.PaymentIntent;
//import com.stripe.model.Event;
//import com.stripe.net.Webhook;
//import com.stripe.param.PaymentIntentCreateParams;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//
//@Service
//@RequiredArgsConstructor
//public class PaymentServiceImpl implements PaymentService {
//
//    private final PaymentRepository paymentRepository;
//    private final StripeConfig stripeConfig;
//
//    @Value("${stripe.secret.key}")
//    private String stripeSecretKey;
//
//    @Override
//    public Map<String, String> createPaymentOrder(int amount) {
//        try {
//            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
//                    .setAmount((long) amount * 100) // Stripe uses cents
//                    .setCurrency("INR")
//                    .addPaymentMethodType("upi")
//                    .build();
//
//            PaymentIntent paymentIntent = PaymentIntent.create(params);
//
//            Payment payment = Payment.builder()
//                    .paymentMethod("STRIPE")
//                    .stripePaymentIntentId(paymentIntent.getId())
//                    .amount((double) amount)
//                    .status("CREATED")
//                    .currency("INR")
//                    .paymentTime(LocalDateTime.now())
//                    .build();
//
//            paymentRepository.save(payment);
//
//            Map<String, String> response = new HashMap<>();
//            response.put("paymentIntentId", paymentIntent.getId());
//            response.put("clientSecret", paymentIntent.getClientSecret());
//            response.put("amount", String.valueOf(amount));
//            response.put("currency", "INR");
//
//            return response;
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to create payment intent in Stripe", e);
//        }
//    }

//    @Override
//    public boolean confirmPayment(String payload, String stripeSignature) {
//        try {
//            Event event = Webhook.constructEvent(payload, stripeSignature, stripeSecretKey);
//
//            if ("payment_intent.succeeded".equals(event.getType())) {
//                String paymentIntentId = event.getData().getObject().get("id").toString();
//                Optional<Payment> paymentOpt = paymentRepository.findByStripePaymentIntentId(paymentIntentId);
//                if (paymentOpt.isPresent()) {
//                    Payment payment = paymentOpt.get();
//                    payment.setStatus("SUCCESS");
//                    paymentRepository.save(payment);
//                    return true;
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return false;
//    }

//    @Override
//    public boolean confirmPayment(String payload, String stripeSignature) {
//        try {
//            Event event = Webhook.constructEvent(payload, stripeSignature, stripeSecretKey);
//
//            if ("payment_intent.succeeded".equals(event.getType())) {
//                // Use EventDataObjectDeserializer to extract the StripeObject
//                EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
//                if (dataObjectDeserializer.getObject().isPresent()) {
//                    PaymentIntent paymentIntent = (PaymentIntent) dataObjectDeserializer.getObject().get();
//                    String paymentIntentId = paymentIntent.getId();
//
//                    Optional<Payment> paymentOpt = paymentRepository.findByStripePaymentIntentId(paymentIntentId);
//                    if (paymentOpt.isPresent()) {
//                        Payment payment = paymentOpt.get();
//                        payment.setStatus("SUCCESS");
//                        paymentRepository.save(payment);
//                        return true;
//                    }
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return false;
//    }
//}