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
    private final PaymentEmailService paymentEmailService;

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

    @Value("${payos.checkout-url-prefix:https://pay.payos.vn/web/}")
    private String checkoutUrlPrefix;

    @Value("${payment.webhook-retry-max-attempts:5}")
    private int webhookRetryMaxAttempts;

    @Value("${payment.webhook-retry-delay-ms:500}")
    private long webhookRetryDelayMs;

    public PayOSService(
            PayOS payOS,
            PaymentRepo paymentRepo,
            UserRepo userRepo,
            PlanRepo planRepo,
            CreditService creditService,
            QRCodeService qrCodeService,
            OrderCodeGenerator orderCodeGenerator,
            PaymentEmailService paymentEmailService) {
        this.payOS = payOS;
        this.paymentRepo = paymentRepo;
        this.userRepo = userRepo;
        this.planRepo = planRepo;
        this.creditService = creditService;
        this.qrCodeService = qrCodeService;
        this.orderCodeGenerator = orderCodeGenerator;
        this.paymentEmailService = paymentEmailService;
    }

    /**
     * Create payment with pessimistic lock to prevent race condition.
     * Uses database lock on user's pending payments to ensure only one payment is
     * created at a time.
     */
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

        // ========== REUSE LOGIC WITH PESSIMISTIC LOCK ==========
        // Sử dụng pessimistic lock để tránh race condition khi 2 request đồng thời
        if (reusePendingPayment) {
            List<Payment> pendingPayments = paymentRepo.findPendingPaymentsByUserIdWithLock(userId);
            log.debug("Found {} pending payments for user {}", pendingPayments.size(), userId);

            if (!pendingPayments.isEmpty()) {
                // Tìm pending payment cùng gói
                Payment matchingPending = pendingPayments.stream()
                        .filter(p -> p.getPlan() != null && p.getPlan().getCode().equals(planCode))
                        .findFirst()
                        .orElse(null);

                // Nếu có pending payment cùng gói và còn mới (trong vòng spamBlockMinutes)
                if (matchingPending != null) {
                    LocalDateTime createdAt = matchingPending.getCreatedAt();
                    boolean isRecent = createdAt != null &&
                            createdAt.isAfter(LocalDateTime.now().minusMinutes(spamBlockMinutes));

                    log.info("Found pending payment: orderCode={}, user={}, plan={}, createdAt={}, isRecent={}",
                            matchingPending.getOrderCode(), userId, planCode, createdAt, isRecent);

                    if (isRecent) {
                        try {
                            // Lấy payment info từ PayOS để check status
                            var paymentInfo = payOS.paymentRequests().get(matchingPending.getOrderCode());
                            String statusName = paymentInfo.getStatus() != null ? paymentInfo.getStatus().name() : null;

                            log.info("PayOS status for orderCode={}: {}", matchingPending.getOrderCode(), statusName);

                            if ("PENDING".equals(statusName) || "PROCESSING".equals(statusName)) {
                                // Payment link vẫn còn active → REUSE
                                // Lấy checkoutUrl và qrCode đã lưu trong database
                                String checkoutUrl = matchingPending.getCheckoutUrl();
                                String qrCode = matchingPending.getQrCode();

                                // Fallback nếu không có trong DB (payment cũ trước khi có feature này)
                                if (checkoutUrl == null || checkoutUrl.isBlank()) {
                                    checkoutUrl = buildCheckoutUrl(matchingPending.getOrderCode());
                                }
                                if (qrCode == null || qrCode.isBlank()) {
                                    log.warn(
                                            "No saved QR code found, generating from checkout URL (may not be scannable by bank app)");
                                    qrCode = generateQRCodeSafe(checkoutUrl, matchingPending.getOrderCode());
                                }

                                log.info("REUSING payment link: orderCode={}, url={}, hasQR={}",
                                        matchingPending.getOrderCode(), checkoutUrl, qrCode != null);

                                return new CreatePaymentResponse(
                                        checkoutUrl,
                                        String.valueOf(matchingPending.getOrderCode()),
                                        qrCode,
                                        (long) plan.getPrice(),
                                        plan.getName());
                            } else {
                                // Payment đã expired/cancelled → Tạo mới
                                log.info("Payment link expired/cancelled (status={}), will create new one", statusName);
                                matchingPending.setStatus("EXPIRED");
                                paymentRepo.save(matchingPending);
                            }

                        } catch (Exception e) {
                            // Không lấy được payment từ PayOS - có thể đã hết hạn
                            log.warn("Cannot get payment from PayOS (orderCode={}): {}",
                                    matchingPending.getOrderCode(), e.getMessage());

                            // Đánh dấu payment cũ là EXPIRED và tạo mới
                            matchingPending.setStatus("EXPIRED");
                            paymentRepo.save(matchingPending);
                            log.info("Marked old payment as EXPIRED, will create new one");
                        }
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

            String checkoutUrl = paymentLink.getCheckoutUrl();
            String qrCode = paymentLink.getQrCode();
            String qrCodeToSave = qrCode; // Lưu QR gốc từ PayOS (VietQR string hoặc URL)

            // Fallback: Generate QR code if PayOS doesn't provide one
            if (qrCode == null || qrCode.isBlank()) {
                log.info("PayOS did not provide QR code, generating our own");
                try {
                    qrCode = qrCodeService.generateQRCodeBase64(checkoutUrl);
                    // Không lưu base64 vào DB vì quá lớn (~50KB)
                    // Khi reuse sẽ generate lại từ checkoutUrl
                    qrCodeToSave = null;
                    log.debug("Generated fallback QR code for orderCode: {}", orderCode);
                } catch (Exception e) {
                    log.warn("Failed to generate fallback QR code: {}", e.getMessage());
                }
            }

            // Lưu checkoutUrl và qrCode vào database để reuse sau này
            // Chỉ lưu QR code nếu là VietQR string (ngắn), không lưu base64 (quá lớn)
            payment.setCheckoutUrl(checkoutUrl);
            if (qrCodeToSave != null && !qrCodeToSave.startsWith("data:image")) {
                payment.setQrCode(qrCodeToSave);
            }
            paymentRepo.save(payment);
            log.debug("Saved checkoutUrl and qrCode for orderCode: {}", orderCode);

            return new CreatePaymentResponse(
                    checkoutUrl,
                    String.valueOf(orderCode),
                    qrCode,
                    (long) plan.getPrice(),
                    plan.getName());

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

    /**
     * Build checkout URL from order code (configurable prefix)
     */
    private String buildCheckoutUrl(long orderCode) {
        return checkoutUrlPrefix + orderCode;
    }

    /**
     * Generate QR code safely with error handling
     */
    private String generateQRCodeSafe(String checkoutUrl, long orderCode) {
        try {
            log.debug("Generating QR code for orderCode={}", orderCode);
            return qrCodeService.generateQRCodeBase64(checkoutUrl);
        } catch (Exception e) {
            log.error("Failed to generate QR code for orderCode={}: {}", orderCode, e.getMessage());
            return null;
        }
    }

    /**
     * Handle webhook with PRODUCTION-SAFE flow.
     * 
     * STATE MACHINE:
     * - PENDING → PAID → CREDITED (happy path)
     * - PENDING → PAID → PAID_CREDIT_FAILED (credit add failed, will be retried by
     * job)
     * - PENDING → FAILED/CANCELLED (PayOS code != "00", no money received)
     * 
     * SAFETY GUARANTEES:
     * 1. Atomic claim via UPDATE query - only ONE thread processes each webhook
     * 2. Never set FAILED when money is received (code="00")
     * 3. No exceptions thrown to prevent PayOS infinite retry
     * 4. Credits are added AFTER payment status is set to PAID
     */
    @Transactional
    public void handleWebhook(Map<String, Object> webhookData) {
        long orderCode = 0;
        try {
            log.info("========== PayOS WEBHOOK START ==========");
            log.info("Webhook data: {}", webhookData);

            // Step 1: Verify signature - throws exception if invalid
            var verifiedData = payOS.webhooks().verify(webhookData);

            orderCode = verifiedData.getOrderCode();
            String code = verifiedData.getCode();

            log.info("Verified webhook: orderCode={}, code={}", orderCode, code);

            // Step 2: Load payment FIRST (with retry for race condition with createPayment)
            // CRITICAL: Ensure payment exists before claiming webhook
            Payment payment = findPaymentWithRetry(orderCode);

            // Step 3: ATOMIC CLAIM - prevents race condition
            // Only ONE thread can claim and process this webhook
            // IMPORTANT: Claim AFTER confirming payment exists
            int claimed = paymentRepo.claimWebhook(orderCode);
            if (claimed == 0) {
                log.info("Webhook already claimed/processed for orderCode: {} - skipping", orderCode);
                return;
            }
            log.info("Successfully claimed webhook for orderCode: {}", orderCode);

            // Step 4: Check if already in final state (extra safety)
            if (payment.isSuccessful()) {
                log.warn("Payment already successful for orderCode: {} - skipping", orderCode);
                return;
            }

            // Step 5: Process based on PayOS response code
            if ("00".equals(code)) {
                // ========== PAYMENT SUCCESS - money received from user ==========
                String transactionRef = verifiedData.getReference();
                processSuccessfulPayment(payment, transactionRef);
            } else {
                // ========== PAYMENT FAILED - no money received ==========
                processFailedPayment(payment, code);
            }

            log.info("========== PayOS WEBHOOK END ==========");

        } catch (Exception e) {
            // Log error but DON'T throw - we don't want PayOS to retry infinitely
            log.error("Webhook processing error for orderCode={}: {}", orderCode, e.getMessage(), e);
            // Payment status already updated in try block, no rollback needed
        }
    }

    /**
     * Process successful payment (PayOS code="00" = money received)
     * 
     * FLOW: PENDING → PAID → CREDITED (or PAID_CREDIT_FAILED)
     * 
     * CRITICAL: Never set FAILED here - user already paid!
     * 
     * @param payment        Payment entity
     * @param transactionRef PayOS transaction reference ID
     */
    private void processSuccessfulPayment(Payment payment, String transactionRef) {
        long orderCode = payment.getOrderCode();

        // Step 1: Mark as PAID first (money received, credits not yet added)
        payment.setStatus(Payment.STATUS_PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setTransactionId(transactionRef);
        paymentRepo.save(payment);
        log.info("Payment marked as PAID: orderCode={}", orderCode);

        // Step 2: Try to add credits
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
                    expiresAt);

            // Step 3a: Credits added successfully → CREDITED
            payment.setStatus(Payment.STATUS_CREDITED);
            paymentRepo.save(payment);
            log.info("Payment CREDITED successfully: orderCode={}, chatCredits={}, quizCredits={}",
                    orderCode, plan.getChatCredits(), plan.getQuizGenCredits());

            // Send success email (async)
            paymentEmailService.sendPaymentSuccessEmail(payment);

        } catch (Exception e) {
            // Step 3b: Credit add failed → PAID_CREDIT_FAILED (NOT FAILED!)
            // CRITICAL: User already paid, do NOT set FAILED
            log.error("Failed to add credits for PAID orderCode={}: {}", orderCode, e.getMessage(), e);

            payment.setStatus(Payment.STATUS_PAID_CREDIT_FAILED);
            payment.incrementCreditRetryCount();
            paymentRepo.save(payment);

            log.warn("Payment marked as PAID_CREDIT_FAILED: orderCode={} - will be retried by scheduled job",
                    orderCode);
            // Don't throw - let the retry job handle it
        }
    }

    /**
     * Process failed payment (PayOS code != "00" = no money received)
     * 
     * Safe to mark as FAILED because user didn't pay.
     */
    private void processFailedPayment(Payment payment, String code) {
        payment.setStatus(Payment.STATUS_FAILED);
        paymentRepo.save(payment);
        log.warn("Payment FAILED: orderCode={}, code={} (no money received)", payment.getOrderCode(), code);
    }

    /**
     * Find payment with retry mechanism.
     * Handles race condition when webhook arrives before createPayment transaction
     * commits.
     */
    private Payment findPaymentWithRetry(long orderCode) {
        for (int attempt = 1; attempt <= webhookRetryMaxAttempts; attempt++) {
            var paymentOpt = paymentRepo.findByOrderCodeWithLock(orderCode);

            if (paymentOpt.isPresent()) {
                log.info("Payment found on attempt {}: orderCode={}", attempt, orderCode);
                return paymentOpt.get();
            }

            if (attempt < webhookRetryMaxAttempts) {
                long waitTime = webhookRetryDelayMs * attempt;
                log.info("Payment not found, retrying in {}ms (attempt {}/{})",
                        waitTime, attempt, webhookRetryMaxAttempts);
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new BadRequestException("Webhook processing interrupted");
                }
            }
        }

        throw new NotFoundException("Payment not found after " + webhookRetryMaxAttempts +
                " attempts: orderCode=" + orderCode);
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

            if ("CREDITED".equals(payment.getStatus())) {  // SUCCESS deprecated
                response.put("paidAt", payment.getPaidAt());
                response.put("chatCredits", plan.getChatCredits());
                response.put("quizCredits", plan.getQuizGenCredits());
            }
        }

        log.info("Payment status details: orderCode={}, status={}", orderCode, payment.getStatus());
        return response;
    }

    /**
     * Get payment details for continuing payment (with QR code).
     * Only works for PENDING payments owned by the user.
     */
    @Transactional(readOnly = true)
    public CreatePaymentResponse getPaymentForContinue(long orderCode, Long userId) {
        Payment payment = paymentRepo.findByOrderCodeWithPlan(orderCode)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng: " + orderCode));

        // Check ownership
        if (!payment.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Bạn không có quyền truy cập đơn hàng này");
        }

        // Check status
        if (!"PENDING".equals(payment.getStatus())) {
            throw new BadRequestException("Đơn hàng không ở trạng thái chờ thanh toán");
        }

        // Check if payment is still valid on PayOS
        try {
            var paymentInfo = payOS.paymentRequests().get(orderCode);
            String statusName = paymentInfo.getStatus() != null ? paymentInfo.getStatus().name() : null;

            if (!"PENDING".equals(statusName) && !"PROCESSING".equals(statusName)) {
                // Payment đã hết hạn trên PayOS
                payment.setStatus("EXPIRED");
                paymentRepo.save(payment);
                throw new BadRequestException("Đơn hàng đã hết hạn. Vui lòng tạo đơn hàng mới.");
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Cannot verify payment on PayOS: {}", e.getMessage());
            // Nếu không check được PayOS, vẫn cho phép tiếp tục (có thể do network issue)
        }

        // Get checkout URL and QR code
        String checkoutUrl = payment.getCheckoutUrl();
        String qrCode = payment.getQrCode();

        // Fallback if not saved in DB
        if (checkoutUrl == null || checkoutUrl.isBlank()) {
            checkoutUrl = buildCheckoutUrl(orderCode);
        }
        if (qrCode == null || qrCode.isBlank()) {
            log.warn("No saved QR code for orderCode={}, generating from URL", orderCode);
            qrCode = generateQRCodeSafe(checkoutUrl, orderCode);
        }

        Plan plan = payment.getPlan();

        log.info("Returning payment for continue: orderCode={}, hasQR={}", orderCode, qrCode != null);

        return new CreatePaymentResponse(
                checkoutUrl,
                String.valueOf(orderCode),
                qrCode,
                payment.getAmount().longValue(),
                plan != null ? plan.getName() : null);
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
                        payment.getPlan().getDurationMonths()))
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
                } else {
                    log.debug("Skipping payment {} - will retry later: {}", payment.getOrderCode(), errorMsg);
                }
                processed++;
            }
        }

        log.info("Stale payment cleanup completed. Processed: {}, Expired: {}, Cancelled: {}",
                processed, expired, cancelled);
    }

    /**
     * Cleanup old failed/expired/cancelled payments to prevent database bloat.
     * Runs daily at 3 AM. Deletes payments older than 30 days that are not CREDITED.
     * NOTE: SUCCESS status is deprecated, only CREDITED indicates full success
     */
    @Scheduled(cron = "0 0 3 * * ?") // Chạy lúc 3:00 AM mỗi ngày
    @Transactional
    public void cleanupOldFailedPayments() {
        log.info("Running old payment cleanup task...");

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        // Chỉ xóa các payment không thành công và đã quá 30 ngày
        List<String> statusesToDelete = List.of("EXPIRED", "CANCELLED", "FAILED");
        int totalDeleted = 0;

        for (String status : statusesToDelete) {
            try {
                List<Payment> oldPayments = paymentRepo.findByStatusAndCreatedAtBefore(status, thirtyDaysAgo);

                if (!oldPayments.isEmpty()) {
                    // Xóa theo batch để tránh lock quá lâu
                    int batchSize = Math.min(oldPayments.size(), 100);
                    List<Payment> toDelete = oldPayments.subList(0, batchSize);

                    paymentRepo.deleteAll(toDelete);
                    totalDeleted += toDelete.size();

                    log.info("Deleted {} old {} payments (>30 days)", toDelete.size(), status);
                }
            } catch (Exception e) {
                log.error("Failed to delete old {} payments: {}", status, e.getMessage());
            }
        }

        if (totalDeleted > 0) {
            log.info("Old payment cleanup completed. Total deleted: {}", totalDeleted);
        } else {
            log.info("No old payments to delete");
        }
    }
}
