package com.htai.exe201phapluatso.payment.service;

import com.htai.exe201phapluatso.auth.entity.Plan;
import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.repo.PlanRepo;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.common.exception.NotFoundException;
import com.htai.exe201phapluatso.credit.service.CreditService;
import com.htai.exe201phapluatso.payment.entity.Payment;
import com.htai.exe201phapluatso.payment.repo.PaymentRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {
    
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    
    private final PaymentRepo paymentRepo;
    private final UserRepo userRepo;
    private final PlanRepo planRepo;
    private final CreditService creditService;
    private final VNPayService vnPayService;

    public PaymentService(
            PaymentRepo paymentRepo,
            UserRepo userRepo,
            PlanRepo planRepo,
            CreditService creditService,
            VNPayService vnPayService
    ) {
        this.paymentRepo = paymentRepo;
        this.userRepo = userRepo;
        this.planRepo = planRepo;
        this.creditService = creditService;
        this.vnPayService = vnPayService;
    }

    /**
     * Create payment and generate VNPay URL
     */
    @Transactional
    public String createPayment(Long userId, String planCode, String ipAddress) {
        // Validate input
        if (planCode == null || planCode.isBlank()) {
            throw new com.htai.exe201phapluatso.common.exception.BadRequestException("Mã gói không được để trống");
        }
        
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        
        Plan plan = planRepo.findByCode(planCode)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói: " + planCode));
        
        // Generate unique transaction reference
        String txnRef = "PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);

        
        // Create payment record
        Payment payment = new Payment();
        payment.setUser(user);
        payment.setPlan(plan);
        payment.setAmount(BigDecimal.valueOf(plan.getPrice()));
        payment.setVnpTxnRef(txnRef);
        payment.setStatus("PENDING");
        payment.setIpAddress(ipAddress);
        paymentRepo.save(payment);
        
        log.info("Created payment: txnRef={}, user={}, plan={}, amount={}", 
                txnRef, userId, planCode, plan.getPrice());
        
        // Generate VNPay payment URL (NO spaces, NO special chars to avoid signature issues)
        String orderInfo = "Payment_" + planCode;
        return vnPayService.createPaymentUrl(txnRef, BigDecimal.valueOf(plan.getPrice()), orderInfo, ipAddress);
    }

    /**
     * Process VNPay callback
     */
    @Transactional
    public void processPaymentCallback(String txnRef, String responseCode, 
                                      String transactionNo, String bankCode, String cardType,
                                      String vnpAmount) {
        log.info("=== Processing Payment Callback ===");
        log.info("TxnRef: {}", txnRef);
        log.info("ResponseCode: {}", responseCode);
        log.info("TransactionNo: {}", transactionNo);
        log.info("Amount: {}", vnpAmount);
        
        Payment payment = paymentRepo.findByVnpTxnRef(txnRef)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + txnRef));
        
        log.info("Found payment: id={}, status={}, amount={}", payment.getId(), payment.getStatus(), payment.getAmount());
        
        // Check if already processed (prevent double callback)
        if ("SUCCESS".equals(payment.getStatus())) {
            log.warn("⚠️ Payment already processed: {}", txnRef);
            return;
        }
        
        // Validate amount (security check)
        BigDecimal receivedAmount = new BigDecimal(vnpAmount).divide(new BigDecimal(100));
        log.info("Amount validation: expected={}, received={}", payment.getAmount(), receivedAmount);
        
        if (payment.getAmount().compareTo(receivedAmount) != 0) {
            log.error("❌ Amount mismatch: expected={}, received={}", payment.getAmount(), receivedAmount);
            payment.setStatus("FAILED");
            paymentRepo.save(payment);
            throw new IllegalStateException("Amount mismatch");
        }
        
        if ("00".equals(responseCode)) {
            // Payment success
            log.info("✅ Payment SUCCESS - updating status and adding credits");
            
            payment.setStatus("SUCCESS");
            payment.setPaidAt(LocalDateTime.now());
            payment.setVnpTransactionNo(transactionNo);
            payment.setVnpBankCode(bankCode);
            payment.setVnpCardType(cardType);
            paymentRepo.save(payment);
            
            log.info("Payment record updated: txnRef={}, transactionNo={}", txnRef, transactionNo);
            
            // Add credits to user
            Plan plan = payment.getPlan();
            log.info("Plan details: code={}, chatCredits={}, quizCredits={}, duration={}", 
                    plan.getCode(), plan.getChatCredits(), plan.getQuizGenCredits(), plan.getDurationMonths());
            
            LocalDateTime expiresAt = plan.getDurationMonths() > 0 
                    ? LocalDateTime.now().plusMonths(plan.getDurationMonths())
                    : null;
            
            log.info("Adding credits to user: userId={}, expiresAt={}", payment.getUser().getId(), expiresAt);
            
            creditService.addCredits(
                    payment.getUser().getId(),
                    plan.getChatCredits(),
                    plan.getQuizGenCredits(),
                    plan.getCode(),
                    expiresAt
            );
            
            log.info("✅ Credits added successfully: user={}, chat={}, quiz={}", 
                    payment.getUser().getId(), plan.getChatCredits(), plan.getQuizGenCredits());
        } else {
            // Payment failed
            log.warn("❌ Payment FAILED: txnRef={}, responseCode={}", txnRef, responseCode);
            payment.setStatus("FAILED");
            paymentRepo.save(payment);
        }
        
        log.info("=== Payment Callback Processing Complete ===");
    }

    /**
     * Get payment history for user
     */
    public java.util.List<com.htai.exe201phapluatso.payment.dto.PaymentHistoryResponse> getPaymentHistory(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        
        java.util.List<Payment> payments = paymentRepo.findByUserOrderByCreatedAtDesc(user);
        
        return payments.stream()
                .map(payment -> new com.htai.exe201phapluatso.payment.dto.PaymentHistoryResponse(
                        payment.getId(),
                        payment.getPlan().getCode(),
                        payment.getPlan().getName(),
                        payment.getAmount(),
                        payment.getStatus(),
                        payment.getPaymentMethod(),
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
}
