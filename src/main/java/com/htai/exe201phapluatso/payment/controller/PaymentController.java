package com.htai.exe201phapluatso.payment.controller;

import com.htai.exe201phapluatso.auth.security.AuthUserPrincipal;
import com.htai.exe201phapluatso.payment.dto.CreatePaymentRequest;
import com.htai.exe201phapluatso.payment.dto.CreatePaymentResponse;
import com.htai.exe201phapluatso.payment.entity.Payment;
import com.htai.exe201phapluatso.payment.service.IdempotencyService;
import com.htai.exe201phapluatso.payment.service.PayOSService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PayOSService payOSService;
    private final IdempotencyService idempotencyService;

    public PaymentController(PayOSService payOSService, IdempotencyService idempotencyService) {
        this.payOSService = payOSService;
        this.idempotencyService = idempotencyService;
    }

    /**
     * Create payment and get PayOS checkout URL + QR Code
     * POST /api/payment/create
     * 
     * Supports Idempotency-Key header to prevent duplicate payments on network retry.
     * If the same key is used with PENDING/SUCCESS payment, returns existing payment.
     */
    @PostMapping("/create")
    public ResponseEntity<CreatePaymentResponse> createPayment(
            @RequestBody CreatePaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication
    ) {
        AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
        Long userId = principal.userId();

        // Input validation
        if (request == null || request.planCode() == null || request.planCode().isBlank()) {
            throw new IllegalArgumentException("Plan code is required");
        }

        String planCode = request.planCode();

        // Check idempotency key nếu có
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            log.info("Creating PayOS payment with idempotency key: user={}, plan={}, key={}", 
                    userId, planCode, idempotencyKey);
            
            Optional<Payment> existingPayment = idempotencyService.checkIdempotencyKey(
                    userId, idempotencyKey, planCode);
            
            if (existingPayment.isPresent()) {
                Payment payment = existingPayment.get();
                log.info("Returning existing payment from idempotency cache: orderCode={}", 
                        payment.getOrderCode());
                
                // Build response từ existing payment
                return ResponseEntity.ok(new CreatePaymentResponse(
                        payment.getCheckoutUrl(),
                        String.valueOf(payment.getOrderCode()),
                        payment.getQrCode(),
                        payment.getAmount().longValue(),
                        payment.getPlan() != null ? payment.getPlan().getName() : null
                ));
            }
        } else {
            log.info("Creating PayOS payment: user={}, plan={}", userId, planCode);
        }

        // Tạo payment mới
        CreatePaymentResponse response = payOSService.createPayment(userId, planCode);

        // Update idempotency record với payment mới (nếu có key)
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            try {
                Payment newPayment = payOSService.getPaymentByOrderCode(
                        Long.parseLong(response.orderCode()));
                idempotencyService.updateIdempotencyRecord(userId, idempotencyKey, newPayment);
            } catch (Exception e) {
                log.warn("Failed to update idempotency record: {}", e.getMessage());
                // Không throw exception - payment đã được tạo thành công
            }
        }

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
        log.debug("Webhook data: {}", webhookData);

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

            // Process real webhook (includes signature verification)
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
     * Note: This endpoint is public for polling from frontend
     * Only returns minimal info (status, amount) - no sensitive data
     */
    @GetMapping("/status/{orderCode}")
    public ResponseEntity<Map<String, Object>> checkPaymentStatus(
            @PathVariable long orderCode,
            Authentication authentication
    ) {
        log.debug("Checking payment status: orderCode={}", orderCode);

        try {
            Map<String, Object> response = payOSService.getPaymentStatusDetails(orderCode);
            
            // Optional: verify user owns this payment if authenticated
            if (authentication != null && authentication.getPrincipal() instanceof AuthUserPrincipal) {
                AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
                Long userId = principal.userId();
                
                // Get payment to check ownership
                Payment payment = payOSService.getPaymentByOrderCode(orderCode);
                if (!payment.getUser().getId().equals(userId)) {
                    // Return minimal info for non-owner (just status for polling)
                    Map<String, Object> minimalResponse = new HashMap<>();
                    minimalResponse.put("orderCode", orderCode);
                    minimalResponse.put("status", response.get("status"));
                    return ResponseEntity.ok(minimalResponse);
                }
            }
            
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
     * Get payment details with QR code for continuing payment
     * GET /api/payment/continue/{orderCode}
     */
    @GetMapping("/continue/{orderCode}")
    public ResponseEntity<?> getPaymentForContinue(
            @PathVariable long orderCode,
            Authentication authentication
    ) {
        AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
        Long userId = principal.userId();

        log.info("Getting payment for continue: orderCode={}, user={}", orderCode, userId);

        try {
            CreatePaymentResponse response = payOSService.getPaymentForContinue(orderCode, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get payment for continue", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
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
