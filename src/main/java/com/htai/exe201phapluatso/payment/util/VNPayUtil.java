package com.htai.exe201phapluatso.payment.util;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class VNPayUtil {
    
    /**
     * Build query string from params (sorted by key)
     * URL encode ALL values (VNPay standard)
     */
    public static String buildQueryString(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    try {
                        String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString());
                        return entry.getKey() + "=" + encodedValue;
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.joining("&"));
    }
    
    /**
     * Generate HMAC SHA512 signature
     */
    public static String hmacSHA512(String key, String data) {
        return new HmacUtils(HmacAlgorithms.HMAC_SHA_512, key).hmacHex(data);
    }
    
    /**
     * Build hash data string (sorted by key, URL encoded values)
     * Used for BOTH creating payment and verifying IPN
     * VNPay standard: key=URLEncoder.encode(value)
     */
    public static String buildHashData(Map<String, String> params) {
        Map<String, String> sortedParams = new TreeMap<>(params);
        sortedParams.remove("vnp_SecureHash");
        sortedParams.remove("vnp_SecureHashType");
        
        return sortedParams.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .map(entry -> {
                    try {
                        String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString());
                        return entry.getKey() + "=" + encodedValue;
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.joining("&"));
    }
}
