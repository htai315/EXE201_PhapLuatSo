package com.htai.exe201phapluatso.payment.service;

import com.htai.exe201phapluatso.payment.config.VNPayConfig;
import com.htai.exe201phapluatso.payment.util.VNPayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VNPayService {
    
    private static final Logger log = LoggerFactory.getLogger(VNPayService.class);
    private final VNPayConfig vnPayConfig;

    public VNPayService(VNPayConfig vnPayConfig) {
        this.vnPayConfig = vnPayConfig;
    }

    /**
     * Create VNPay payment URL
     */
    public String createPaymentUrl(String txnRef, BigDecimal amount, String orderInfo, String ipAddress) {
        Map<String, String> vnpParams = new HashMap<>();
        
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amount.multiply(new BigDecimal(100)).longValue()));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", ipAddress);
        
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_CreateDate", vnpCreateDate);
        
        cld.add(Calendar.MINUTE, 15);
        String vnpExpireDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);
        
        // Build hash data and generate signature (VNPay standard: key=URLEncoder.encode(value))
        String hashData = VNPayUtil.buildHashData(vnpParams);
        log.debug("Hash data: {}", hashData);
        
        String vnpSecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData);
        log.debug("Generated signature: {}", vnpSecureHash);
        
        // Build query string (same as hash data - URL encode all values)
        String queryUrl = VNPayUtil.buildQueryString(vnpParams);
        
        // Add signature to query string
        String paymentUrl = vnPayConfig.getVnpayUrl() + "?" + queryUrl + "&vnp_SecureHash=" + vnpSecureHash;
        
        log.info("Payment URL created for txnRef: {}", txnRef);
        
        return paymentUrl;
    }

    /**
     * Verify VNPay callback signature
     */
    public boolean verifySignature(Map<String, String> params) {
        String vnpSecureHash = params.get("vnp_SecureHash");
        if (vnpSecureHash == null) {
            return false;
        }
        
        String hashData = VNPayUtil.buildHashData(params);
        String calculatedHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData);
        
        log.debug("VNPay signature verification: received={}, calculated={}", vnpSecureHash, calculatedHash);
        
        // Use equalsIgnoreCase because VNPay may return uppercase hex
        return vnpSecureHash.equalsIgnoreCase(calculatedHash);
    }
}
