package com.htai.exe201phapluatso.common.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.htai.exe201phapluatso.common.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Service for uploading files to Cloudinary
 */
@Service
public class CloudinaryService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryService.class);

    private final Cloudinary cloudinary;

    // Allowed image types for avatar upload
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    // Allowed document types for legal documents
    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    );

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final long MAX_DOCUMENT_SIZE = 10 * 1024 * 1024; // 10MB

    public CloudinaryService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret
    ) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
        log.info("Cloudinary service initialized with cloud name: {}", cloudName);
    }

    /**
     * Upload an image (e.g., avatar) to Cloudinary
     *
     * @param file   The image file to upload
     * @param folder The folder in Cloudinary (e.g., "avatars")
     * @return The secure URL of the uploaded image
     */
    public String uploadImage(MultipartFile file, String folder) {
        validateFile(file, ALLOWED_IMAGE_TYPES, MAX_IMAGE_SIZE, "ảnh (JPG, PNG, GIF, WEBP)");

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "image"
            ));

            String secureUrl = (String) result.get("secure_url");
            log.info("Image uploaded to Cloudinary: {}", secureUrl);
            return secureUrl;

        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary", e);
            throw new BadRequestException("Không thể tải lên ảnh. Vui lòng thử lại.");
        }
    }

    /**
     * Upload a document (e.g., PDF, DOCX) to Cloudinary
     *
     * @param file   The document file to upload
     * @param folder The folder in Cloudinary (e.g., "legal-documents")
     * @return The secure URL of the uploaded document
     */
    public String uploadDocument(MultipartFile file, String folder) {
        validateFile(file, ALLOWED_DOCUMENT_TYPES, MAX_DOCUMENT_SIZE, "tài liệu (PDF, DOCX, TXT)");

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "raw"
            ));

            String secureUrl = (String) result.get("secure_url");
            log.info("Document uploaded to Cloudinary: {}", secureUrl);
            return secureUrl;

        } catch (IOException e) {
            log.error("Failed to upload document to Cloudinary", e);
            throw new BadRequestException("Không thể tải lên tài liệu. Vui lòng thử lại.");
        }
    }

    /**
     * Delete a file from Cloudinary by its public ID
     *
     * @param publicId The public ID of the file (extracted from URL)
     */
    public void deleteFile(String publicId) {
        if (publicId == null || publicId.isEmpty()) {
            return;
        }

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("File deleted from Cloudinary: {}", publicId);
        } catch (IOException e) {
            log.warn("Failed to delete file from Cloudinary: {}", publicId, e);
        }
    }

    /**
     * Extract public ID from Cloudinary URL
     * Example: https://res.cloudinary.com/dyj0jywh9/image/upload/v123/avatars/abc.jpg
     * Returns: avatars/abc
     */
    public String extractPublicId(String cloudinaryUrl) {
        if (cloudinaryUrl == null || !cloudinaryUrl.contains("cloudinary.com")) {
            return null;
        }

        try {
            // Find the upload/ part and get everything after it
            int uploadIndex = cloudinaryUrl.indexOf("/upload/");
            if (uploadIndex == -1) {
                return null;
            }

            String afterUpload = cloudinaryUrl.substring(uploadIndex + 8);

            // Remove version if present (v123456/)
            if (afterUpload.startsWith("v") && afterUpload.contains("/")) {
                afterUpload = afterUpload.substring(afterUpload.indexOf("/") + 1);
            }

            // Remove file extension
            int dotIndex = afterUpload.lastIndexOf(".");
            if (dotIndex != -1) {
                afterUpload = afterUpload.substring(0, dotIndex);
            }

            return afterUpload;
        } catch (Exception e) {
            log.warn("Failed to extract public ID from URL: {}", cloudinaryUrl);
            return null;
        }
    }

    /**
     * Validate file before upload
     */
    private void validateFile(MultipartFile file, Set<String> allowedTypes, long maxSize, String typeDescription) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File không được để trống");
        }

        if (file.getSize() > maxSize) {
            throw new BadRequestException("Kích thước file không được vượt quá " + (maxSize / 1024 / 1024) + "MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new BadRequestException("File phải là " + typeDescription);
        }

        // Validate filename to prevent path traversal
        String filename = file.getOriginalFilename();
        if (filename != null && (filename.contains("..") || filename.contains("/") || filename.contains("\\"))) {
            throw new BadRequestException("Tên file không hợp lệ");
        }
    }
}
