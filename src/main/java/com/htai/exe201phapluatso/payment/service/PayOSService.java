package com.htai.exe201phapluatso.payment.service;

import com.htai.exe201phapluatso.auth.entity.Plan;
import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.repo.PlanRepo;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.common.exception.BadRequestException;
import com.htai.exe201phapluatso.common.exception.ForbiddenException;
import com.htai.exe201phapluatso.common.exception.NotFoundException;
import com.htai.exe201phapluatso.credit.service.CreditService;
import com.htai.exe201phapluatso.payment.dto.CreatePaymentResponse;
import com.htai.exe201phapluatso.payment.dto.PaymentHistoryResponse;
import com.htai.exe201phapluatso.payment.entity.Payment;
import com.htai.exe201phapluatso.payment.repo.PaymentRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PayOSService {

    private static final Logger log = LoggerFactory.getLogger(PayOSService.class);

    private final PayOS payOS;
    private final PaymentRepo paymentRepo;
    private final UserRepo userRepo;
    private final PlanRepo planRepo;
    private final CreditService creditService;
    private final QRCodeService qrCodeService;
    private final OrderCodeGenerator orderCodeGenerator;

    @Value("${payos.return-url}")
    private String returnUrl;

    @Value("${payos.cancel-url}")
    private String cancelUrl;

    @Value("${payment.spam-block-minutes:10}")
    private int spamBlockMinutes;

    @Value("${payment.stale-payment-minutes:30}")
    private int stalePaymentMinutes;

    @Value("${payment.max-cleanup-batch-size:20}")
    private int maxCleanupBatchSize;

    @Value("${payment.max-retries:3}")
    private int maxRetries;

    @Value("${payment.retry-base-delay-ms:500}")
    private long retryBaseDelayMs;

    @Value("${payment.test-mode:false}")
    private boolean testMode;

    @Value("${payment.reuse-pending-payment:true}")
    private boolean reusePendingPayment;

    public PayOSService(
            PayOS payOS,
            PaymentRepo paymentRepo,
            UserRepo userRepo,
            PlanRepo planRepo,
            CreditService creditService,
            QRCodeService qrCodeService,
            OrderCodeGenerator orderCodeGenerator
    ) {
        this.payOS = payOS;
        this.paymentRepo = paymentRepo;
        this.userRepo = userRepo;
        this.planRepo = planRepo;
        this.creditService = creditService;
        this.qrCodeService = qrCodeService;
        this.orderCodeGenerator = orderCodeGenerator;
    }

    @Transactional
    public CreatePaymentResponse createPayment(Long userId, String planCode) {
        if (planCode == null || planCode.isBlank()) {
            throw new BadRequestException("Mã gói không được để trống");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        Plan plan = planRepo.findByCode(planCode)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói: " + planCode));

        if (plan.getPrice() <= 0) {
            throw new BadRequestException("Gói không hợp lệ");
        }

        // ========== REUSE LOGIC START ==========
        // Kiểm tra xem có pending payment cùng gói không (plan đã được JOIN FETCH)
        List<Payment> pendingPayments = paymentRepo.findByUserAndStatusOrderByCreatedAtDesc(user, "PENDING");
        
        if (!pendingPayments.isEmpty() && reusePendingPayment) {
            // Tìm pending payment cùng gói (không chỉ lấy mới nhất)
            Payment matchingPending = null;
            for (Payment pending : pendingPayments) {
                if (pending.getPlan().getCode().equals(planCode)) {
                    matchingPending = pending;
                    break;
                }
            }
            
            // Nếu có pending payment cùng gói và còn mới (trong vòng spamBlockMinutes)
            if (matchingPending != null) {
                LocalDateTime createdAt = matchingPending.getCreatedAt();
                boolean isRecent = createdAt.isAfter(LocalDateTime.now().minusMinutes(spamBlockMinutes));
                
                if (isRecent) {
                    log.info("Found recent pending payment: orderCode={}, user={}, plan={}", 
                            matchingPending.getOrderCode(), userId, planCode);
                    
                    try {
                        // Lấy payment info từ PayOS để check status
                        var paymentInfo = payOS.paymentRequests().get(matchingPending.getOrderCode());
                        String statusName = paymentInfo.getStatus() != null ? paymentInfo.getStatus().name() : null;
                        
                        if ("PENDING".equals(statusName) || "PROCESSING".equals(statusName)) {
                            // Payment link vẫn còn active → REUSE
                            // Note: PaymentLink from get() doesn't have checkoutUrl/qrCode
                            // We need to construct them manually
                            String checkoutUrl = "https://pay.payos.vn/web/" + matchingPending.getOrderCode();
                            
                            log.info("REUSING payment link: orderCode={}, url={}", 
                                    matchingPending.getOrderCode(), checkoutUrl);
                            
                            // Generate QR code from checkout URL
                            String qrCode = null;
                            try {
                                log.info("Generating QR code for reused payment: orderCode={}, url={}", 
                                        matchingPending.getOrderCode(), checkoutUrl);
                                qrCode = qrCodeService.generateQRCodeBase64(checkoutUrl);
                                log.info("QR code generated successfully for reused payment: orderCode={}", 
                                        matchingPending.getOrderCode());
                            } catch (Exception e) {
                                log.error("Failed to generate QR code for reused payment: orderCode={}", 
                                         matchingPending.getOrderCode(), e);
                            }
                            
                            return new CreatePaymentResponse(
                                    checkoutUrl,
                                    String.valueOf(matchingPending.getOrderCode()),
                                    qrCode,
                                    (long) plan.getPrice(),
                                    plan.getName()
                            );
                        } else {
                            // Payment đã expired/cancelled → Tạo mới
                            log.info("Payment link expired/cancelled, will create new one");
                            matchingPending.setStatus("EXPIRED");
                            paymentRepo.save(matchingPending);
                        }
                        
                    } catch (Exception e) {
                        // Không lấy được payment từ PayOS
                        log.warn("Cannot get payment from PayOS: {}", e.getMessage());
                        
                        if (!testMode) {
                            // Production: block spam
                            throw new BadRequestException(
                                "Bạn đã có giao dịch đang chờ xử lý. " +
                                "Vui lòng hoàn tất thanh toán hoặc đợi " + spamBlockMinutes + " phút."
                            );
                        }
                        // Test mode: cho phép tạo mới
                    }
                }
            }
        }
        // ========== REUSE LOGIC END ==========

        // Tạo payment mới với unique order code từ database sequence
        long orderCode = orderCodeGenerator.generateOrderCode();

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setPlan(plan);
        payment.setAmount(BigDecimal.valueOf(plan.getPrice()));
        payment.setOrderCode(orderCode);
        payment.setVnpTxnRef("PAYOS_" + orderCode);
        payment.setStatus("PENDING");
        payment.setPaymentMethod("PAYOS");
        payment.setWebhookProcessed(false);
        paymentRepo.save(payment);

        log.info("Created payment: orderCode={}, user={}, plan={}, amount={}",
                orderCode, userId, planCode, plan.getPrice());

        try {
            String description = "Don hang " + orderCode;
            
            log.info("========== CREATING PAYOS PAYMENT ==========");
            log.info("OrderCode: {}", orderCode);
            log.info("Amount: {}", plan.getPrice());
            log.info("Description: {}", description);
            log.info("ReturnUrl: {}", returnUrl);
            log.info("CancelUrl: {}", cancelUrl);
            
            CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount((long) plan.getPrice())
                    .description(description)
                    .cancelUrl(cancelUrl)
                    .returnUrl(returnUrl)
                    .build();

            var paymentLink = callPayOSWithRetry(request, maxRetries);

            log.info("PayOS Response: checkoutUrl={}, qrCode={}, paymentLinkId={}", 
                    paymentLink.getCheckoutUrl(), 
                    paymentLink.getQrCode(),
                    paymentLink.getPaymentLinkId());
            log.info("========== PAYOS PAYMENT CREATED ==========");

            String qrCode = paymentLink.getQrCode();
            
            // Fallback: Generate QR code if PayOS doesn't provide one
            if (qrCode == null || qrCode.isBlank()) {
                log.info("PayOS did not provide QR code, generating our own");
                try {
                    qrCode = qrCodeService.generateQRCodeBase64(paymentLink.getCheckoutUrl());
                    log.debug("Generated fallback QR code for orderCode: {}", orderCode);
                } catch (Exception e) {
                    log.warn("Failed to generate fallback QR code: {}", e.getMessage());
                }
            }

            return new CreatePaymentResponse(
                    paymentLink.getCheckoutUrl(),
                    String.valueOf(orderCode),
                    qrCode,
                    (long) plan.getPrice(),
                    plan.getName()
            );

        } catch (Exception e) {
            log.error("========== PAYOS PAYMENT FAILED ==========");
            log.error("Error: {}", e.getMessage());
            payment.setStatus("FAILED");
            paymentRepo.save(payment);
            throw new BadRequestException("Không thể tạo link thanh toán: " + e.getMessage());
        }
    }

    private vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse callPayOSWithRetry(
            CreatePaymentLinkRequest request, int maxRetries) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("Calling PayOS API... (attempt {}/{})", attempt, maxRetries);
                return payOS.paymentRequests().create(request);
            } catch (Exception e) {
                lastException = e;
                log.warn("PayOS API call failed (attempt {}/{}): {}", attempt, maxRetries, e.getMessage());
                
                String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (errorMsg.contains("invalid") || errorMsg.contains("unauthorized") || 
                    errorMsg.contains("duplicate") || errorMsg.contains("already exists")) {
                    throw e;
                }
                
                if (attempt < maxRetries) {
                    long waitTime = retryBaseDelayMs * (1L << (attempt - 1));
                    log.info("Waiting {}ms before retry...", waitTime);
                    Thread.sleep(waitTime);
                }
            }
        }
        
        throw lastException;
    }

    @Transactional
    public void handleWebhook(Map<String, Object> webhookData) {
        try {
            log.info("========== PayOS WEBHOOK START ==========");
            log.info("Webhook data: {}", webhookData);

            var verifiedData = payOS.webhooks().verify(webhookData);
            
            long orderCode = verifiedData.getOrderCode();
            String code = verifiedData.getCode();

            log.info("Verified webhook: orderCode={}, code={}", orderCode, code);

            Payment payment = paymentRepo.findByOrderCodeWithLock(orderCode)
                    .orElseThrow(() -> new NotFoundException("Payment not found: " + orderCode));

            if (payment.getWebhookProcessed() != null && payment.getWebhookProcessed()) {
                log.warn("Webhook already processed for orderCode: {}", orderCode);
                return;
            }

            if ("SUCCESS".equals(payment.getStatus()) || "FAILED".equals(payment.getStatus())) {
                log.warn("Payment already in final state: {} for orderCode: {}", payment.getStatus(), orderCode);
                payment.setWebhookProcessed(true);
                paymentRepo.save(payment);
                return;
            }

            if ("00".equals(code)) {
                Plan plan = payment.getPlan();
                LocalDateTime expiresAt = plan.getDurationMonths() > 0
                        ? LocalDateTime.now().plusMonths(plan.getDurationMonths())
                        : null;

                try {
                    creditService.addCredits(
                            payment.getUser().getId(),
                            plan.getChatCredits(),
                            plan.getQuizGenCredits(),
                            plan.getCode(),
                            expiresAt
                    );
                    log.info("Credits added successfully for orderCode: {}", orderCode);
                } catch (Exception e) {
                    log.error("Failed to add credits for orderCode: {}", orderCode, e);
                    payment.setStatus("FAILED");
                    payment.setWebhookProcessed(true);
                    paymentRepo.save(payment);
                    throw new BadRequestException("Failed to add credits: " + e.getMessage());
                }

                payment.setStatus("SUCCESS");
                payment.setPaidAt(LocalDateTime.now());
                payment.setTransactionId(verifiedData.getReference());
                payment.setWebhookProcessed(true);
                paymentRepo.save(payment);

                log.info("Payment SUCCESS: orderCode={}, credits added", orderCode);
            } else {
                payment.setStatus("FAILED");
                payment.setWebhookProcessed(true);
                paymentRepo.save(payment);
                log.warn("Payment FAILED: orderCode={}, code={}", orderCode, code);
            }

            log.info("========== PayOS WEBHOOK END ==========");

        } catch (NotFoundException e) {
            log.error("Payment not found in webhook", e);
            throw e;
        } catch (Exception e) {
            log.error("Webhook processing failed", e);
            throw new BadRequestException("Invalid webhook: " + e.getMessage());
        }
    }

    public boolean verifyWebhookSignature(Map<String, Object> webhookData) {
        try {
            payOS.webhooks().verify(webhookData);
            return true;
        } catch (Exception e) {
            log.error("Webhook signature verification failed", e);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByOrderCode(long orderCode) {
        return paymentRepo.findByOrderCodeWithPlan(orderCode)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + orderCode));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentStatusDetails(long orderCode) {
        Payment payment = paymentRepo.findByOrderCode(orderCode)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + orderCode));

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("orderCode", orderCode);
        response.put("status", payment.getStatus());
        response.put("amount", payment.getAmount());
        
        Plan plan = payment.getPlan();
        if (plan != null) {
            response.put("planCode", plan.getCode());
            response.put("planName", plan.getName());
            
            if ("SUCCESS".equals(payment.getStatus())) {
                response.put("paidAt", payment.getPaidAt());
                response.put("chatCredits", plan.getChatCredits());
                response.put("quizCredits", plan.getQuizGenCredits());
            }
        }

        log.info("Payment status details: orderCode={}, status={}", orderCode, payment.getStatus());
        return response;
    }

    public List<PaymentHistoryResponse> getPaymentHistory(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        List<Payment> payments = paymentRepo.findByUserOrderByCreatedAtDesc(user);
        
        if (payments.size() > 50) {
            log.info("User {} has {} payments, returning last 50 only", userId, payments.size());
            payments = payments.subList(0, 50);
        }

        return payments.stream()
                .map(payment -> new PaymentHistoryResponse(
                        payment.getId(),
                        payment.getPlan().getCode(),
                        payment.getPlan().getName(),
                        payment.getAmount(),
                        payment.getStatus(),
                        payment.getPaymentMethod(),
                        payment.getOrderCode() != null ? String.valueOf(payment.getOrderCode()) : null,
                        payment.getTransactionId(),
                        payment.getVnpTxnRef(),
                        payment.getVnpTransactionNo(),
                        payment.getVnpBankCode(),
                        payment.getVnpCardType(),
                        payment.getCreatedAt(),
                        payment.getPaidAt(),
                        payment.getPlan().getChatCredits(),
                        payment.getPlan().getQuizGenCredits(),
                        payment.getPlan().getDurationMonths()
                ))
                .toList();
    }

    @Transactional
    public void cancelPayment(long orderCode, Long userId) {
        Payment payment = paymentRepo.findByOrderCode(orderCode)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng: " + orderCode));

        if (!payment.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Bạn không có quyền hủy đơn hàng này");
        }

        if (!"PENDING".equals(payment.getStatus())) {
            throw new BadRequestException("Chỉ có thể hủy đơn hàng đang chờ thanh toán");
        }

        try {
            log.info("Cancelling PayOS payment: orderCode={}", orderCode);
            payOS.paymentRequests().cancel(orderCode, "Cancelled by user");
            
            payment.setStatus("CANCELLED");
            paymentRepo.save(payment);
            
            log.info("Payment cancelled successfully: orderCode={}", orderCode);
        } catch (Exception e) {
            log.error("Failed to cancel payment: orderCode={}", orderCode, e);
            throw new BadRequestException("Không thể hủy đơn hàng: " + e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 300000) // Chạy mỗi 5 phút
    @Transactional
    public void cleanupStalePendingPayments() {
        log.info("Running stale payment cleanup task...");
        
        LocalDateTime staleTime = LocalDateTime.now().minusMinutes(stalePaymentMinutes);
        LocalDateTime oneDayAgo = LocalDateTime.now().minusHours(24);
        
        List<Payment> stalePayments = paymentRepo.findByStatusAndCreatedAtBefore("PENDING", staleTime);
        
        if (stalePayments.isEmpty()) {
            log.info("No stale payments found");
            return;
        }
        
        // Tăng batch size lên 50
        int batchSize = Math.min(stalePayments.size(), 50);
        if (stalePayments.size() > batchSize) {
            log.info("Found {} stale payments, processing first {} only", stalePayments.size(), batchSize);
            stalePayments = stalePayments.subList(0, batchSize);
        }
        
        int processed = 0;
        int expired = 0;
        int cancelled = 0;
        
        for (Payment payment : stalePayments) {
            try {
                var paymentInfo = payOS.paymentRequests().get(payment.getOrderCode());
                var status = paymentInfo.getStatus();
                String statusName = status != null ? status.name() : null;
                
                if ("CANCELLED".equals(statusName) || "EXPIRED".equals(statusName)) {
                    payment.setStatus("EXPIRED");
                    paymentRepo.save(payment);
                    expired++;
                    log.info("Marked payment {} as EXPIRED (PayOS status: {})", payment.getOrderCode(), statusName);
                } else if ("PAID".equals(statusName)) {
                    log.warn("Found PAID payment without webhook: {}", payment.getOrderCode());
                    payment.setStatus("NEEDS_REVIEW");
                    paymentRepo.save(payment);
                } else if ("PENDING".equals(statusName) || "PROCESSING".equals(statusName)) {
                    // Nếu payment quá cũ (hơn 24h) mà vẫn PENDING trên PayOS → đánh dấu EXPIRED
                    if (payment.getCreatedAt().isBefore(oneDayAgo)) {
                        payment.setStatus("EXPIRED");
                        paymentRepo.save(payment);
                        expired++;
                        log.info("Marked old pending payment {} as EXPIRED (>24h)", payment.getOrderCode());
                    }
                }
                processed++;
            } catch (Exception e) {
                String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                
                // PayOS không tìm thấy payment → EXPIRED
                if (errorMsg.contains("không tồn tại") || errorMsg.contains("not found") || 
                    errorMsg.contains("tạm dừng") || errorMsg.contains("404")) {
                    payment.setStatus("EXPIRED");
                    paymentRepo.save(payment);
                    expired++;
                    log.info("Marked payment {} as EXPIRED (not found on PayOS)", payment.getOrderCode());
                } 
                // Payment quá cũ (>24h) → EXPIRED luôn
                else if (payment.getCreatedAt().isBefore(oneDayAgo)) {
                    payment.setStatus("EXPIRED");
                    paymentRepo.save(payment);
                    expired++;
                    log.info("Marked payment {} as EXPIRED (>24h old)", payment.getOrderCode());
                } 
                // Lỗi khác nhưng payment đã quá 2 giờ → EXPIRED
                else if (payment.getCreatedAt().isBefore(LocalDateTime.now().minusHours(2))) {
                    payment.setStatus("EXPIRED");
                    paymentRepo.save(payment);
                    expired++;
                    log.info("Marked payment {} as EXPIRED (>2h old, API error)", payment.getOrderCode());
                }
                else {
                    log.debug("Skipping payment {} - will retry later: {}", payment.getOrderCode(), errorMsg);
                }
                processed++;
            }
        }
        
        log.info("Stale payment cleanup completed. Processed: {}, Expired: {}, Cancelled: {}", 
                processed, expired, cancelled);
    }
}
