package com.HomeHive.Controller;

import com.HomeHive.dto.PaymentOrderRequestDTO;
import com.HomeHive.dto.PaymentResponseDTO;
import com.HomeHive.dto.PaymentStatsDTO;
import com.HomeHive.dto.PaymentVerificationDTO;
import com.HomeHive.entities.Payment;
import com.HomeHive.enums.PaymentStatus;
import com.HomeHive.service.PaymentService;
import com.HomeHive.service.RazorpayService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final RazorpayService razorpayService;

    @PostMapping("/create-order")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<PaymentResponseDTO> createPaymentOrder(@Valid @RequestBody PaymentOrderRequestDTO request) {
        PaymentResponseDTO response = razorpayService.createOrder(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<Map<String, Object>> verifyPayment(@Valid @RequestBody PaymentVerificationDTO verificationDTO) {

        try {
            Payment payment = razorpayService.verifyPayment(verificationDTO);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Payment verified successfully",
                "payment", payment
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Payment verification failed: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/my-payments")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<List<Payment>> getMyPayments() {
        List<Payment> payments = paymentService.getResidentPayments();
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/my-payments/status/{status}")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<List<Payment>> getMyPaymentsByStatus(@PathVariable PaymentStatus status) {
        List<Payment> payments = paymentService.getResidentPaymentsByStatus(status);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("hasRole('RESIDENT') or hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    public ResponseEntity<Payment> getPaymentById(@PathVariable Long paymentId) {
        Payment payment = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/bill/{billId}")
    @PreAuthorize("hasRole('RESIDENT') or hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    public ResponseEntity<Payment> getPaymentByBillId(@PathVariable Long billId) {
        try {
            Payment payment = paymentService.getPaymentByBillId(billId);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    public ResponseEntity<List<Payment>> getAllPayments() {
        List<Payment> payments = paymentService.getAllPayments();
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    public ResponseEntity<List<Payment>> getPaymentsByStatus(@PathVariable PaymentStatus status) {
        List<Payment> payments = paymentService.getPaymentsByStatus(status);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    public ResponseEntity<PaymentStatsDTO> getPaymentStats() {
        PaymentStatsDTO stats = paymentService.getPaymentStats();
        return ResponseEntity.ok(stats);
    }

    @PutMapping("/{paymentId}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    public ResponseEntity<Payment> updatePaymentStatus(
            @PathVariable Long paymentId,
            @RequestParam PaymentStatus status,
            @RequestParam(required = false) String reason) {
        
        Payment updatedPayment = paymentService.updatePaymentStatus(paymentId, status, reason);
        return ResponseEntity.ok(updatedPayment);
    }

    @PostMapping("/capture/{paymentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    public ResponseEntity<Payment> capturePayment(
            @PathVariable String paymentId,
            @RequestParam Double amount) {
        
        Payment capturedPayment = razorpayService.capturePayment(paymentId, amount);
        return ResponseEntity.ok(capturedPayment);
    }

    @PostMapping("/refund/{paymentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    public ResponseEntity<Map<String, Object>> refundPayment(
            @PathVariable String paymentId,
            @RequestParam Double amount) {
        
        try {
            String refundId = razorpayService.refundPayment(paymentId, amount);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Refund processed successfully",
                "refundId", refundId
            ));
        } catch (Exception e) {   
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Refund processing failed: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/razorpay/details/{paymentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    public ResponseEntity<String> getRazorpayPaymentDetails(@PathVariable String paymentId) {
        String paymentDetails = razorpayService.getPaymentDetails(paymentId);
        return ResponseEntity.ok(paymentDetails);
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload) {
        return ResponseEntity.ok("OK");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "success", false,
            "message", "An error occurred: " + e.getMessage()
        ));
    }
}
