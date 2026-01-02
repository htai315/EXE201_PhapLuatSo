package com.htai.exe201phapluatso.ai.service;

import com.htai.exe201phapluatso.common.exception.BadRequestException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Service
public class DocumentParserService {

    private static final Logger log = LoggerFactory.getLogger(DocumentParserService.class);

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_TEXT_LENGTH = 100000; // 100K characters - reduced for better AI processing
    
    // Allowed content types
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain"
    );

    public String extractText(MultipartFile file) {
        validateFile(file);

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BadRequestException("Không thể xác định loại file");
        }
        
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Chỉ hỗ trợ file PDF, DOCX, TXT");
        }

        try {
            log.info("Extracting text from file: type={}, size={} bytes", contentType, file.getSize());
            
            String text = switch (contentType) {
                case "application/pdf" -> extractFromPDF(file);
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> extractFromDOCX(file);
                case "text/plain" -> extractFromTXT(file);
                default -> throw new BadRequestException("Chỉ hỗ trợ file PDF, DOCX, TXT");
            };

            if (text.isBlank()) {
                throw new BadRequestException("File không có nội dung văn bản");
            }

            // Limit text length for better AI processing
            if (text.length() > MAX_TEXT_LENGTH) {
                log.info("Text truncated from {} to {} characters", text.length(), MAX_TEXT_LENGTH);
                text = text.substring(0, MAX_TEXT_LENGTH);
            }

            log.info("Extracted {} characters from document", text.length());
            return text.trim();
        } catch (BadRequestException e) {
            throw e;
        } catch (IOException e) {
            log.error("Error reading file", e);
            throw new BadRequestException("Không thể đọc file. Vui lòng thử lại.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File không được để trống");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File không được vượt quá 10MB");
        }
        
        // Validate filename
        String filename = file.getOriginalFilename();
        if (filename != null && filename.contains("..")) {
            throw new BadRequestException("Tên file không hợp lệ");
        }
    }

    private String extractFromPDF(MultipartFile file) throws IOException {
        // Use BufferedInputStream for better memory handling
        try (InputStream is = new BufferedInputStream(file.getInputStream());
             PDDocument document = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractFromDOCX(MultipartFile file) throws IOException {
        try (InputStream is = new BufferedInputStream(file.getInputStream());
             XWPFDocument document = new XWPFDocument(is)) {
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append("\n");
            }
            return text.toString();
        }
    }

    private String extractFromTXT(MultipartFile file) throws IOException {
        // Read with buffered stream for large files
        try (InputStream is = new BufferedInputStream(file.getInputStream())) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
