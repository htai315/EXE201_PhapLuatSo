package com.htai.exe201phapluatso.payment.controller;

import com.htai.exe201phapluatso.auth.security.AuthUserPrincipal;
import com.htai.exe201phapluatso.payment.dto.CreatePaymentRequest;
import com.htai.exe201phapluatso.payment.dto.CreatePaymentResponse;
import com.htai.exe201phapluatso.payment.entity.Payment;
import com.htai.exe201phapluatso.payment.service.PayOSService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PayOSService payOSService;

    public PaymentController(PayOSService payOSService) {
        this.payOSService = payOSService;
    }

    /**
     * Create payment and get PayOS checkout URL + QR Code
     * POST /api/payment/create
     */
    @PostMapping("/create")
    public ResponseEntity<CreatePaymentResponse> createPayment(
            @RequestBody CreatePaymentRequest request,
            Authentication authentication
    ) {
        AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
        Long userId = principal.userId();

        // Input validation
        if (request == null || request.planCode() == null || request.planCode().isBlank()) {
            throw new IllegalArgumentException("Plan code is required");
        }

        log.info("Creating PayOS payment: user={}, plan={}", userId, request.planCode());

        CreatePaymentResponse response = payOSService.createPayment(userId, request.planCode());

        return ResponseEntity.ok(response);
    }

    /**
     * PayOS Webhook callback
     * POST /api/payment/webhook
     * Fixed: Handle PayOS test webhook and real webhooks
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody Map<String, Object> webhookData,
            HttpServletRequest request
    ) {
        log.info("========== PayOS WEBHOOK RECEIVED ==========");
        log.info("Remote IP: {}", request.getRemoteAddr());
        log.info("Webhook data: {}", webhookData);

        Map<String, String> response = new HashMap<>();

        try {
            // Check if this is a PayOS test webhook
            // Test webhook has data.orderCode = 123 (fixed test value)
            Object dataObj = webhookData.get("data");
            if (dataObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) dataObj;
                Object orderCodeObj = data.get("orderCode");
                // PayOS test webhook always uses orderCode = 123
                if (orderCodeObj != null && "123".equals(orderCodeObj.toString())) {
                    log.info("PayOS test webhook detected (orderCode=123) - responding with success");
                    response.put("code", "00");
                    response.put("message", "Success");
                    return ResponseEntity.ok(response);
                }
            }

            // For real webhooks, verify signature
            if (!payOSService.verifyWebhookSignature(webhookData)) {
                log.warn("Invalid webhook signature from IP: {}", request.getRemoteAddr());
                response.put("code", "99");
                response.put("message", "Invalid signature");
                return ResponseEntity.status(400).body(response);
            }

            payOSService.handleWebhook(webhookData);
            response.put("code", "00");
            response.put("message", "Success");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Webhook processing failed", e);
            response.put("code", "99");
            response.put("message", "Error: " + e.getMessage());
            // Return 200 to prevent PayOS from retrying (we've logged the error)
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Check payment status by orderCode
     * GET /api/payment/status/{orderCode}
     */
    @GetMapping("/status/{orderCode}")
    public ResponseEntity<Map<String, Object>> checkPaymentStatus(@PathVariable long orderCode) {
        log.info("Checking payment status: orderCode={}", orderCode);

        try {
            Map<String, Object> response = payOSService.getPaymentStatusDetails(orderCode);
            log.info("Payment status response: orderCode={}, status={}", orderCode, response.get("status"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking payment status: orderCode={}", orderCode, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("orderCode", orderCode);
            errorResponse.put("status", "ERROR");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Get payment history for current user
     * GET /api/payment/history
     */
    @GetMapping("/history")
    public ResponseEntity<?> getPaymentHistory(Authentication authentication) {
        AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
        Long userId = principal.userId();

        log.info("Getting payment history for user: {}", userId);

        var history = payOSService.getPaymentHistory(userId);
        return ResponseEntity.ok(history);
    }

    /**
     * Cancel a pending payment
     * Fixed: Added user authorization check
     * POST /api/payment/cancel/{orderCode}
     */
    @PostMapping("/cancel/{orderCode}")
    public ResponseEntity<Map<String, String>> cancelPayment(
            @PathVariable long orderCode,
            Authentication authentication
    ) {
        AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
        Long userId = principal.userId();

        log.info("Cancelling payment: orderCode={}, user={}", orderCode, userId);

        Map<String, String> response = new HashMap<>();
        try {
            payOSService.cancelPayment(orderCode, userId);
            response.put("status", "success");
            response.put("message", "Payment cancelled");
        } catch (Exception e) {
            log.error("Failed to cancel payment", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}
