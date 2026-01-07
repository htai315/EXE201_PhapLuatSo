package com.htai.exe201phapluatso.payment.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for generating QR codes
 * Used for payment checkout URLs when PayOS doesn't provide QR code
 */
@Service
public class QRCodeService {

    private static final Logger log = LoggerFactory.getLogger(QRCodeService.class);

    /**
     * Generate QR code as base64 data URI
     * 
     * @param data The data to encode (e.g., checkout URL)
     * @param width QR code width in pixels
     * @param height QR code height in pixels
     * @return Base64 data URI (data:image/png;base64,...)
     * @throws RuntimeException if QR code generation fails
     */
    public String generateQRCodeBase64(String data, int width, int height) {
        try {
            log.debug("Generating QR code: width={}, height={}, dataLength={}", width, height, data.length());
            
            // Configure QR code hints
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            // Generate QR code matrix
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height, hints);

            // Convert to PNG image
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            // Encode to base64
            byte[] qrCodeBytes = outputStream.toByteArray();
            String base64Image = Base64.getEncoder().encodeToString(qrCodeBytes);
            
            log.debug("QR code generated successfully: size={}KB", qrCodeBytes.length / 1024);
            
            return "data:image/png;base64," + base64Image;
            
        } catch (WriterException e) {
            log.error("Failed to encode QR code data", e);
            throw new RuntimeException("Không thể tạo mã QR: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("Failed to write QR code image", e);
            throw new RuntimeException("Không thể tạo ảnh QR: " + e.getMessage(), e);
        }
    }

    /**
     * Generate QR code with default size (280x280)
     * 
     * @param data The data to encode
     * @return Base64 data URI
     */
    public String generateQRCodeBase64(String data) {
        return generateQRCodeBase64(data, 280, 280);
    }
}
