package com.htai.exe201phapluatso.ai.service;

import com.htai.exe201phapluatso.common.exception.BadRequestException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class DocumentParserService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_TEXT_LENGTH = 1000000; // 1M characters (for legal documents)

    public String extractText(MultipartFile file) {
        validateFile(file);

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BadRequestException("Không thể xác định loại file");
        }

        try {
            String text = switch (contentType) {
                case "application/pdf" -> extractFromPDF(file);
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> extractFromDOCX(file);
                case "text/plain" -> extractFromTXT(file);
                default -> throw new BadRequestException("Chỉ hỗ trợ file PDF, DOCX, TXT");
            };

            if (text.isBlank()) {
                throw new BadRequestException("File không có nội dung văn bản");
            }

            // Limit text length
            if (text.length() > MAX_TEXT_LENGTH) {
                text = text.substring(0, MAX_TEXT_LENGTH);
            }

            return text.trim();
        } catch (IOException e) {
            throw new BadRequestException("Không thể đọc file: " + e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File không được để trống");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File không được vượt quá 10MB");
        }
    }

    private String extractFromPDF(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractFromDOCX(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append("\n");
            }
            return text.toString();
        }
    }

    private String extractFromTXT(MultipartFile file) throws IOException {
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }
}
