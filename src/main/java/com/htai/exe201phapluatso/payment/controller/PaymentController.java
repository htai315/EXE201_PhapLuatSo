package com.htai.exe201phapluatso.payment.controller;

import com.htai.exe201phapluatso.auth.security.AuthUserPrincipal;
import com.htai.exe201phapluatso.payment.dto.CreatePaymentRequest;
import com.htai.exe201phapluatso.payment.dto.CreatePaymentResponse;
import com.htai.exe201phapluatso.payment.service.PaymentService;
import com.htai.exe201phapluatso.payment.service.VNPayService;
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
    
    private final PaymentService paymentService;
    private final VNPayService vnPayService;

    public PaymentController(PaymentService paymentService, VNPayService vnPayService) {
        this.paymentService = paymentService;
        this.vnPayService = vnPayService;
    }

    /**
     * Create payment and get VNPay URL
     * POST /api/payment/create
     */
    @PostMapping("/create")
    public ResponseEntity<CreatePaymentResponse> createPayment(
            @RequestBody CreatePaymentRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
        Long userId = principal.userId();
        
        String ipAddress = getClientIP(httpRequest);
        
        log.info("Creating payment: user={}, plan={}, ip={}", userId, request.planCode(), ipAddress);
        
        String paymentUrl = paymentService.createPayment(userId, request.planCode(), ipAddress);
        
        // Extract txnRef from URL for frontend reference
        String txnRef = extractTxnRef(paymentUrl);
        
        return ResponseEntity.ok(new CreatePaymentResponse(paymentUrl, txnRef));
    }


    /**
     * VNPay IPN callback (server-to-server)
     * GET /api/payment/vnpay-ipn
     */
    @GetMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> vnpayIPN(@RequestParam Map<String, String> params) {
        log.info("========== VNPay IPN CALLBACK START ==========");
        log.info("Received params: {}", params);
        log.info("Param count: {}", params.size());
        
        Map<String, String> response = new HashMap<>();
        
        // Verify signature
        log.info("Verifying signature...");
        boolean signatureValid = vnPayService.verifySignature(params);
        log.info("Signature valid: {}", signatureValid);
        
        if (!signatureValid) {
            log.error("❌ Invalid VNPay signature");
            response.put("RspCode", "97");
            response.put("Message", "Invalid signature");
            return ResponseEntity.ok(response);
        }
        
        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");
        String bankCode = params.get("vnp_BankCode");
        String cardType = params.get("vnp_CardType");
        String vnpAmount = params.get("vnp_Amount");
        
        log.info("Transaction details: txnRef={}, responseCode={}, transactionNo={}, amount={}", 
                txnRef, responseCode, transactionNo, vnpAmount);
        
        try {
            log.info("Processing payment callback...");
            paymentService.processPaymentCallback(txnRef, responseCode, transactionNo, bankCode, cardType, vnpAmount);
            
            log.info("✅ Payment processed successfully");
            response.put("RspCode", "00");
            response.put("Message", "Success");
        } catch (Exception e) {
            log.error("❌ Error processing payment callback", e);
            response.put("RspCode", "99");
            response.put("Message", "Error: " + e.getMessage());
        }
        
        log.info("========== VNPay IPN CALLBACK END ==========");
        return ResponseEntity.ok(response);
    }

    /**
     * Get client IP address (convert IPv6 localhost to IPv4)
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        String ip;
        if (xfHeader == null) {
            ip = request.getRemoteAddr();
        } else {
            ip = xfHeader.split(",")[0];
        }
        
        // Convert IPv6 localhost to IPv4 to avoid colon characters in signature
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        
        return ip;
    }

    /**
     * Extract txnRef from payment URL
     */
    private String extractTxnRef(String url) {
        try {
            String[] parts = url.split("vnp_TxnRef=");
            if (parts.length > 1) {
                return parts[1].split("&")[0];
            }
        } catch (Exception e) {
            log.warn("Could not extract txnRef from URL", e);
        }
        return "";
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
        
        var history = paymentService.getPaymentHistory(userId);
        return ResponseEntity.ok(history);
    }
}
